# Changelog

All notable changes to the ASR Library will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [1.0.0] - 2025-12-23

### Added
- Initial release of ASR Library as a reusable Android module
- Public API via `AsrLibrary` facade class
- Offline ASR engines:
  - Moonshine (Sherpa-ONNX Moonshine Tiny INT8 model)
  - Vosk (with external model support)
- Streaming ASR engines:
  - English streaming model with endpoint detection
  - Bilingual Chinese + English streaming
- VAD (Voice Activity Detection) processor using Silero VAD
- Audio recorder with Kotlin Flow support
- Comprehensive documentation:
  - README with API reference
  - Integration guide with examples
  - Sample code for common use cases
- ProGuard/R8 rules for minification
- AndroidManifest with required permissions
- Build configuration for library module

### Features
- Clean, simple public API for easy integration
- Support for both offline batch and real-time streaming ASR
- Built-in audio recording with Flow-based API
- Voice activity detection for speech segmentation
- Multiple ASR engine options (Moonshine, Vosk, Streaming)
- Fully offline operation (after model download)
- Low memory footprint with efficient native implementation

### Documentation
- Complete API documentation with examples
- Step-by-step integration guide
- Troubleshooting section
- Sample code for 6 common use cases
- Build instructions for native libraries

### Architecture
- Package: `com.k2fsa.sherpa.onnx.asr`
- Clean separation of public API and internal implementation
- JNI bindings to sherpa-onnx C++ library
- Resource management with AutoCloseable interfaces
- Coroutine-based audio processing

### Dependencies
- AndroidX Core KTX 1.7.0
- AndroidX AppCompat 1.6.1
- Kotlinx Coroutines Android 1.7.3
- Vosk Android 0.3.47
- Sherpa-ONNX native libraries (bundled)

## [Unreleased]

### Planned
- Additional language models
- Performance optimizations
- More streaming model options
- Enhanced error reporting
- Unit tests for library module
- CI/CD integration for library builds
