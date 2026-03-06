import 'dart:io';
import 'package:google_mlkit_image_labeling/google_mlkit_image_labeling.dart';

class MlLabelingService {
  late final ImageLabeler _labeler;
  
  MlLabelingService() {
    // Use default options with 0.5 confidence threshold for learning
    _labeler = ImageLabeler(
      options: ImageLabelerOptions(confidenceThreshold: 0.5),
    );
  }
  
  Future<List<ImageLabel>> analyzeImage(String imagePath) async {
    try {
      final inputImage = InputImage.fromFilePath(imagePath);
      final labels = await _labeler.processImage(inputImage);
      return labels;
    } catch (e) {
      throw Exception('Failed to analyze image: $e');
    }
  }
  
  void dispose() {
    _labeler.close();
  }
}
