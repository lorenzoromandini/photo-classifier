import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/services/preference_service.dart';

final preferenceServiceProvider = Provider((ref) => PreferenceService());

final confidenceThresholdProvider = StateNotifierProvider<ConfidenceThresholdNotifier, double>((ref) {
  return ConfidenceThresholdNotifier();
});

class ConfidenceThresholdNotifier extends StateNotifier<double> {
  final PreferenceService _prefs = PreferenceService();
  
  ConfidenceThresholdNotifier() : super(0.9) {
    _loadValue();
  }
  
  Future<void> _loadValue() async {
    await _prefs.init();
    state = _prefs.getConfidenceThreshold();
  }
  
  Future<void> setValue(double value) async {
    await _prefs.init();
    await _prefs.setConfidenceThreshold(value);
    state = value;
  }
}
