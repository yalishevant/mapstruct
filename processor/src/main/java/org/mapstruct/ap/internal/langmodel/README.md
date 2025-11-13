# MapStruct LangModel Thin Layer

This module encapsulates the descriptor-first API that decouples the mapper creation pipeline from
`javax.lang.model`. The key pieces introduced here:

- `LangModelContext` – lifecycle-bound entry point that exposes descriptor factories, neutral
  element/type utilities, diagnostics and generated-file sinks.
- Descriptor contracts (`TypeDescriptor`, `TypeElementDescriptor`, `AnnotationDescriptor`, etc.) that
  provide the minimal metadata MapStruct needs without leaking compiler-specific handles.
- Backend service contract `LangModelContextFactory` plus the default `javax` implementation under
  `org.mapstruct.ap.internal.langmodel.javax` used by the existing annotation processor.
- Optional capabilities for bridging existing SPIs (builder introspection, enum mapping, mapping
  exclusion, SPI bridge) in a controlled fashion.

The thin layer is validated by `LangModelDescriptorParityTest`, which runs the javax backend against a
set of curated fixtures and compares the snapshot with a golden file. Any regression in the descriptor
surface or semantics fails the test, making it safe to continue slicing dependencies off `javax.lang.model`.
