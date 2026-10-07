# PhotoEditPro - Advanced Android Photo Editor

A CapCut-inspired Android photo editor built with Kotlin and Jetpack Compose featuring professional-grade editing tools.

## Features

### 📸 Core Features
- **Home Screen** - Beautiful onboarding UI
- **Crop Tool** - Multiple aspect ratios (1:1, 16:9, 9:16)
- **Filters** - 7+ premium color filters
- **Adjustments** - Brightness, Contrast, Saturation, Rotation, Zoom
- **Beauty Effects** - Blur, Glow, Smooth, Radiant beauty filters
- **Text Overlay** - Add custom text with color selection
- **Stickers** - 8+ emoji stickers with placement control
- **Export** - High-quality JPEG export to Gallery

### 🎨 Premium UI
- Dark theme with gradient backgrounds
- Smooth animations and transitions
- Modern rounded cards and buttons
- Active filter indicators
- Professional editing panels
- Intuitive tool bar at bottom

### 🚀 Tech Stack
- **Kotlin** - Modern Android development
- **Jetpack Compose** - Declarative UI framework
- **Material 3** - Latest Material Design
- **Coil** - Image loading library
- **Coroutines** - Async operations

## Project Structure

```
app/src/main/
├── AndroidManifest.xml
├── java/com/photoguru/photoeditpro/
│   └── MainActivity.kt          # Main app with all screens
└── res/
    └── (system defaults)
```

## Setup

1. Clone repository
2. Open in Android Studio (Electric Eel or later)
3. Sync Gradle files
4. Run on Android 6.0+ (API 24+) device or emulator

## Permissions

- `READ_MEDIA_IMAGES` - Access gallery photos
- `WRITE_EXTERNAL_STORAGE` - Save edited images

## Editing Workflow

1. **Home** → Select photo from gallery
2. **Crop** → Choose aspect ratio and frame
3. **Filters** → Apply color presets
4. **Adjustments** → Fine-tune brightness, contrast, etc.
5. **Beauty** → Apply beauty effects and blur
6. **Text** → Add custom captions
7. **Stickers** → Decorate with emoji
8. **Export** → Save to Pictures/PhotoEditPro

## Future Enhancements

- [ ] Undo/Redo system
- [ ] Advanced crop with perspective
- [ ] Custom brushes and drawing
- [ ] Layer support
- [ ] Video editing
- [ ] Premium sticker packs
- [ ] Cloud export
- [ ] Batch editing
- [ ] AI-powered auto-enhance
- [ ] Custom filters creator

## License

MIT License - Feel free to use and modify
