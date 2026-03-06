import 'package:shared_preferences/shared_preferences.dart';

class PreferenceService {
  static SharedPreferences? _prefs;
  
  Future<void> init() async {
    _prefs ??= await SharedPreferences.getInstance();
  }
  
  // Confidence threshold
  double getConfidenceThreshold() {
    return _prefs?.getDouble('confidence_threshold') ?? 0.9;
  }
  
  Future<void> setConfidenceThreshold(double value) async {
    await _prefs?.setDouble('confidence_threshold', value);
  }
  
  // Onboarding
  bool getOnboardingComplete() {
    return _prefs?.getBool('onboarding_complete') ?? false;
  }
  
  Future<void> setOnboardingComplete(bool value) async {
    await _prefs?.setBool('onboarding_complete', value);
  }
  
  // Folder URIs
  List<String> getFolderUris() {
    return _prefs?.getStringList('folder_uris') ?? [];
  }
  
  Future<void> addFolderUri(String uri) async {
    final uris = getFolderUris();
    if (!uris.contains(uri)) {
      uris.add(uri);
      await _prefs?.setStringList('folder_uris', uris);
    }
  }
  
  // First launch
  bool getIsFirstLaunch() {
    return _prefs?.getBool('is_first_launch') ?? true;
  }
  
  Future<void> setFirstLaunchComplete() async {
    await _prefs?.setBool('is_first_launch', false);
  }
}
