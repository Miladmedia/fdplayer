# Contributing to FD Player

Thanks for helping! Guidelines:

1. Fork the repo and create a branch: `feature/short-name`.
2. Follow the architecture: `domain/` (models, repository interfaces, use cases) has no Android/data dependencies; `data/` implements it; `ui/` and `service/` consume it through Hilt.
3. Every user-visible string must exist in `values`, `values-fa` and `values-ar`.
4. Run `./gradlew testDebugUnitTest assembleDebug` before opening a pull request.
5. Keep commits small and describe the "why" in the message.

Developer: MiladMedia — https://ble.ir/miladmedia_ir
