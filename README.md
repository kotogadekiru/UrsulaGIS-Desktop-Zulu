# UrsulaGIS Desktop

A comprehensive Geographic Information System (GIS) desktop application built with JavaFX, featuring advanced geospatial analysis capabilities, 3D visualization, and agricultural management tools.

## 🌍 Overview

UrsulaGIS Desktop is a powerful GIS application that combines traditional mapping capabilities with specialized tools for agricultural management, environmental analysis, and spatial data processing. Built on modern Java technologies, it provides a robust platform for both professional GIS users and agricultural professionals.

## ✨ Key Features

### 🗺️ Core GIS Capabilities
- **Multi-format Data Support**: Shapefile, GeoTIFF, and various raster formats
- **Advanced Spatial Analysis**: Geoprocessing tools and spatial operations
- **Coordinate Reference Systems**: EPSG database integration with HSQL backend
- **3D Visualization**: NASA WorldWind integration for immersive 3D mapping

### 🌱 Agricultural Management Tools
- **Crop Management**: Siembra (planting) and Cosecha (harvest) labor tracking
- **Fertilization Planning**: Comprehensive fertilization management system
- **Spraying Operations**: Pulverizacion (spraying) labor management
- **Field Monitoring**: Recorrida (field inspection) and NDVI analysis
- **Soil Analysis**: Soil sampling and analysis tools
- **Margin Analysis**: Financial and yield margin calculations

### 🖥️ User Interface
- **Modern JavaFX Interface**: Responsive and intuitive user experience
- **Multi-language Support**: Internationalization capabilities
- **Drag & Drop**: Easy file import and data management
- **Plugin Architecture**: Extensible system for custom functionality

### 📊 Data Processing
- **Image Processing**: TIFF support with advanced raster operations
- **PDF Generation**: Report creation and export capabilities
- **Excel Integration**: Data import/export with Apache POI
- **QR Code Support**: Barcode generation and scanning

## 🚀 Getting Started

### Prerequisites

- **Java 18** or later with JavaFX support
  - [Azul Zulu JDK with JavaFX](https://www.azul.com/downloads/?version=java-18-sts&package=jdk-fx) (recommended)
  - [Liberica JDK with JavaFX](https://bell-sw.com/pages/downloads/#/java-18-current)
- **Maven 3.8.6** or later
- **Platform-specific tools**:
  - **Windows**: [WiX Toolset 3](https://github.com/wixtoolset/wix3/releases/)
  - **macOS**: XCode (for installer generation)
  - **Linux**: Standard build tools

### Installation

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd UrsulaGIS-Desktop-Zulu
   ```

2. **Build the application**:
   ```bash
   mvn clean install
   ```

3. **Run the application**:
   ```bash
   mvn javafx:run
   ```

### Building Installers

Generate native installers for your platform:

- **Windows**: `mvn clean install` (creates .msi installer)
- **macOS**: `mvn clean install` (creates .dmg installer)
- **Linux**: `mvn clean install` (creates .deb/.rpm packages)

The generated installers will be located in the `target/` directory.

## 🏗️ Architecture

### Technology Stack
- **Java 18**: Core runtime with modern language features
- **JavaFX 24**: Rich client application framework
- **GeoTools 13.6**: Open source GIS library
- **NASA WorldWind 2.0**: 3D globe visualization
- **JOGL**: OpenGL bindings for Java
- **Apache POI**: Microsoft Office document processing
- **PDFBox**: PDF generation and manipulation
- **Google Cloud Translate**: Multi-language support

### Project Structure
```
src/
├── main/
│   ├── java/com/ursulagis/
│   │   ├── desktop/
│   │   │   ├── dao/           # Data Access Objects
│   │   │   ├── gui/           # User Interface
│   │   │   └── plugin/        # Plugin System
│   │   └── ...
│   └── resources/             # Application resources
├── packaging/                 # Platform-specific packaging
└── test/                     # Test suite
```

### Key Components
- **JFXMain**: Main application entry point
- **Plugin System**: Extensible architecture for custom tools
- **Data Access Layer**: Unified data management interface
- **3D Visualization**: WorldWind integration for immersive mapping

## 🔧 Development

### Development Environment Setup

1. **IDE Configuration**: Ensure your IDE points to a JavaFX-enabled JDK
2. **Maven Configuration**: The project uses Maven for dependency management
3. **Plugin Development**: Extend functionality through the plugin system

### Building from Source

```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Generate site documentation
mvn site

# Package without installer
mvn clean package
```

### Debugging

- Use `mvn javafx:run` for development mode
- Check `debug*.log` files for runtime information
- Enable verbose logging in application configuration

## 📦 Dependencies

### Core Dependencies
- **JavaFX**: UI framework and controls
- **GeoTools**: GIS and spatial analysis
- **WorldWind**: 3D globe visualization
- **JOGL**: OpenGL integration
- **Apache POI**: Office document processing
- **PDFBox**: PDF operations
- **Google Cloud**: Translation services

### Development Dependencies
- **Maven**: Build and dependency management
- **Lombok**: Code generation and boilerplate reduction
- **JUnit**: Testing framework

## 🌐 Internationalization

The application supports multiple languages through Google Cloud Translate integration, making it accessible to users worldwide.

## 🔌 Plugin System

UrsulaGIS Desktop features a modular plugin architecture that allows developers to:
- Add new GIS tools and analysis functions
- Integrate with external data sources
- Customize the user interface
- Extend data processing capabilities

## 📱 Platform Support

- **Windows**: Full support with MSI installer
- **macOS**: Native support with DMG installer
- **Linux**: Package-based installation (deb/rpm)

## 🤝 Contributing

We welcome contributions! Please see our contributing guidelines for:
- Code style and standards
- Testing requirements
- Pull request process
- Issue reporting

## 📄 License

This project is licensed under the terms specified in the LICENSE file.

## 🆘 Support

- **Documentation**: Check the `docs/` directory for detailed guides
- **Issues**: Report bugs and feature requests through the issue tracker
- **Community**: Join discussions and get help from the community

## 📈 Roadmap

Future versions will include:
- Enhanced 3D visualization capabilities
- Additional data format support
- Cloud integration features
- Mobile companion applications
- Advanced analytics and machine learning tools

## 🙏 Acknowledgments

- **GeoTools Community**: For the excellent open-source GIS library
- **NASA WorldWind Team**: For 3D globe visualization technology
- **JavaFX Community**: For the modern Java UI framework
- **Open Source Contributors**: For various supporting libraries and tools

---

**UrsulaGIS Desktop** - Empowering GIS professionals with advanced spatial analysis and agricultural management tools.

*Built with ❤️ using Java, JavaFX, and modern GIS technologies.*
