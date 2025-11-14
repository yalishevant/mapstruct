# MapStruct Lang Model & Code Generation Architecture

This document summarises the descriptor-first lang model surface and the new code
emission facade that were introduced while rebuilding the KSP-ready processor
pipeline.

## Descriptor-First Lang Model

* `LangModelContext` now exposes neutral `MapperAnnotation` / `MapperConfigAnnotation`
  contracts instead of leaking backend specific mirrors. Backends supply them via
  `MapperAnnotationAdapter`. All processor layers consume the contracts exclusively.
* `LangModelElementQuery` and `LangModelTypeSystem` were extracted as thin facades
  over `LangModelContext` to make backend capability negotiation explicit and to
  remove raw `javax.lang.model` mirrors from the model layer.
* The thin API snapshot (`thin-api-surface.json`) guards the surface of the
  descriptor contracts so that new capabilities must be negotiated explicitly.

## Model Layer on Descriptors

* The core model (`Type`, `TypeFactory`, mapping method models and option objects)
  now operates purely on `TypeDescriptor` / `ElementDescriptor` instead of
  `TypeMirror`, using `LangModelTypeSystem` and `LangModelElementQuery` as the
  only entry points into the lang model.
* `TypeIntrospector` is responsible for turning descriptors into the richer
  metadata the model layer needs (collection shapes, builder presence, enum
  mapping hints) so that future backends can reuse the same logic.
* SPI implementations still work with `javax` types; descriptor-native adapters
  in the lang model layer bridge between `LangModelContext` and existing SPI
  contracts to preserve binary compatibility.

## Processor Pipeline on Descriptors

* The mapper processors (`MethodRetrievalProcessor`, `MapperCreationProcessor`,
  `MapperRenderingProcessor` and friends) consume descriptor-based context
  objects instead of compiler mirrors; backend-specific details stay behind
  `LangModelContext`.
* `RoundContext` tracks descriptor ids rather than `TypeMirror` instances,
  preventing subtle readiness bugs once different backends no longer share
  mirror identity semantics.

## Annotation Processor Context

* `AnnotationProcessorContext` lives in the processor package and exposes a
  minimal `AnnotationProcessorContextView` to helpers. SPI wiring now happens via
  descriptor-native adapters (`AccessorNamingAdapter`, builder providers, enum
  strategies) so SPI implementors continue to work with `javax` types while the
  core remains backend neutral.
* The default implementations used in tests (`LangModelMinimalBackendContractProcessor`,
  capability factories) were rewritten to satisfy the new contracts and keep the
  validation wall green.

## Code Generation Facade

* `CodeGenerator`, `GeneratedFile`, and `CodeGenerationContext` describe generated
  artifacts without binding to Freemarker APIs. The default implementation lives
  in `org.mapstruct.ap.internal.codegen.freemarker` and is injected via
  `MapperRenderingProcessor` / `MapperServiceProcessor`.
* A `GeneratedFileAccess` abstraction shields the processor from `Filer` details
  and prepares the pipeline for alternative backends (e.g. Kotlin/KSP).

These changes keep the javac processor functional while clearing the path for a
KSP backend that consumes the same descriptor-first model and codegen surface.
