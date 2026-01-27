package org.mapstruct.ksp

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.google.devtools.ksp.getAllSuperTypes
import com.squareup.javapoet.*
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import javax.lang.model.element.Modifier

/**
 * KSP Processor that generates Java mapper implementations from Kotlin interfaces.
 */
class MapperProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(Mapper::class.qualifiedName!!)
        val unprocessed = mutableListOf<KSAnnotated>()

        symbols.forEach { symbol ->
            if (!symbol.validate()) {
                unprocessed.add(symbol)
                return@forEach
            }

            if (symbol is KSClassDeclaration && symbol.classKind == ClassKind.INTERFACE) {
                processMapper(symbol)
            } else {
                logger.error("@Mapper can only be applied to interfaces", symbol)
            }
        }

        return unprocessed
    }

    private fun processMapper(mapper: KSClassDeclaration) {
        val packageName = mapper.packageName.asString()
        val interfaceName = mapper.simpleName.asString()
        val implName = "${interfaceName}Impl"

        logger.info("Generating Java implementation $implName for $interfaceName")

        val javaFile = generateJavaFile(mapper, packageName, implName)

        // Write Java file using KSP's codeGenerator
        val file = codeGenerator.createNewFile(
            Dependencies(true, mapper.containingFile!!),
            packageName,
            implName,
            "java"  // Generate .java file
        )

        OutputStreamWriter(file, StandardCharsets.UTF_8).use { writer ->
            javaFile.writeTo(writer)
        }
    }

    private fun generateJavaFile(mapper: KSClassDeclaration, packageName: String, implName: String): JavaFile {
        val interfaceType = ClassName.get(packageName, mapper.simpleName.asString())

        val classBuilder = TypeSpec.classBuilder(implName)
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(interfaceType)

        // Process each function in the interface
        mapper.getAllFunctions()
            .filter { it.isAbstract }
            .forEach { function ->
                val methodSpec = generateMappingMethod(function)
                if (methodSpec != null) {
                    classBuilder.addMethod(methodSpec)
                }
            }

        return JavaFile.builder(packageName, classBuilder.build())
            .build()
    }

    private fun generateMappingMethod(function: KSFunctionDeclaration): MethodSpec? {
        val params = function.parameters
        if (params.size != 1) {
            logger.warn("Mapping functions should have exactly one parameter: ${function.simpleName.asString()}")
            return null
        }

        val sourceParam = params.first()
        val sourceType = sourceParam.type.resolve()
        val returnType = function.returnType?.resolve() ?: return null

        val sourceClass = sourceType.declaration as? KSClassDeclaration ?: return null
        val targetClass = returnType.declaration as? KSClassDeclaration ?: return null

        // Get @Mapping annotations for custom mappings
        val mappings = function.annotations
            .filter { it.shortName.asString() == "Mapping" }
            .associate { annotation ->
                val source = annotation.arguments.find { it.name?.asString() == "source" }?.value as? String ?: ""
                val target = annotation.arguments.find { it.name?.asString() == "target" }?.value as? String ?: ""
                target to source
            }

        val sourceTypeName = toJavaTypeName(sourceType)
        val returnTypeName = toJavaTypeName(returnType)
        val paramName = sourceParam.name?.asString() ?: "source"

        // Build the method
        val methodBuilder = MethodSpec.methodBuilder(function.simpleName.asString())
            .addAnnotation(Override::class.java)
            .addModifiers(Modifier.PUBLIC)
            .returns(returnTypeName)
            .addParameter(sourceTypeName, paramName)

        // Get target properties
        val targetProperties = getTargetProperties(targetClass)

        // Create new instance and set properties
        methodBuilder.addStatement("\$T result = new \$T()", returnTypeName, returnTypeName)

        targetProperties.forEach { targetName ->
            val sourceName = mappings[targetName] ?: targetName

            // Generate setter call: result.setXxx(source.getXxx())
            val setterName = "set${targetName.replaceFirstChar { it.uppercase() }}"
            val getterName = "get${sourceName.replaceFirstChar { it.uppercase() }}"

            methodBuilder.addStatement("result.\$L(\$L.\$L())", setterName, paramName, getterName)
        }

        methodBuilder.addStatement("return result")

        return methodBuilder.build()
    }

    private fun getTargetProperties(classDecl: KSClassDeclaration): List<String> {
        // First try constructor parameters (for data classes)
        val constructorParams = classDecl.primaryConstructor?.parameters
        if (!constructorParams.isNullOrEmpty()) {
            return constructorParams.mapNotNull { it.name?.asString() }
        }

        // Fallback to declared properties with setters
        return classDecl.getAllProperties()
            .filter { it.isMutable }  // Only var properties (which have setters)
            .map { it.simpleName.asString() }
            .toList()
    }

    private fun toJavaTypeName(type: KSType): TypeName {
        val declaration = type.declaration
        val packageName = declaration.packageName.asString()
        val simpleName = declaration.simpleName.asString()

        // Handle primitive types and common Kotlin types
        return when ("$packageName.$simpleName") {
            "kotlin.String" -> ClassName.get(String::class.java)
            "kotlin.Int" -> if (type.isMarkedNullable) ClassName.get(Integer::class.java) else TypeName.INT
            "kotlin.Long" -> if (type.isMarkedNullable) ClassName.get(java.lang.Long::class.java) else TypeName.LONG
            "kotlin.Double" -> if (type.isMarkedNullable) ClassName.get(java.lang.Double::class.java) else TypeName.DOUBLE
            "kotlin.Float" -> if (type.isMarkedNullable) ClassName.get(java.lang.Float::class.java) else TypeName.FLOAT
            "kotlin.Boolean" -> if (type.isMarkedNullable) ClassName.get(java.lang.Boolean::class.java) else TypeName.BOOLEAN
            "kotlin.Byte" -> if (type.isMarkedNullable) ClassName.get(java.lang.Byte::class.java) else TypeName.BYTE
            "kotlin.Short" -> if (type.isMarkedNullable) ClassName.get(java.lang.Short::class.java) else TypeName.SHORT
            "kotlin.Char" -> if (type.isMarkedNullable) ClassName.get(Character::class.java) else TypeName.CHAR
            "kotlin.Unit" -> TypeName.VOID
            else -> ClassName.get(packageName, simpleName)
        }
    }
}

/**
 * Provider for the MapperProcessor.
 */
class MapperProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return MapperProcessor(environment.codeGenerator, environment.logger)
    }
}
