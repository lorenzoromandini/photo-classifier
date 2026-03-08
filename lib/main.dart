import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:workmanager/workmanager.dart';
import 'presentation/screens/onboarding_screen.dart';
import 'presentation/screens/main_screen.dart';
import 'data/services/preference_service.dart';
import 'data/workers/trash_cleanup_worker.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  
  // Check if onboarding is complete
  final prefs = PreferenceService();
  await prefs.init();
  final isOnboardingComplete = prefs.getOnboardingComplete();
  
  // Initialize WorkManager for background tasks
  await Workmanager().initialize(
    TrashCleanupWorker.callbackDispatcher,
    isInDebugMode: true, // Set to false in production
  );
  
  // Schedule daily trash cleanup if onboarding is complete
  if (isOnboardingComplete) {
    await TrashCleanupWorker.scheduleDailyCleanup();
  }
  
  runApp(
    ProviderScope(
      child: PhotoClassifierApp(
        initialRoute: isOnboardingComplete ? '/main' : '/onboarding',
      ),
    ),
  );
}

class PhotoClassifierApp extends StatelessWidget {
  final String initialRoute;
  
  const PhotoClassifierApp({
    super.key,
    required this.initialRoute,
  });

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Photo Classifier',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: Colors.blue,
          brightness: Brightness.light,
        ),
        useMaterial3: true,
      ),
      darkTheme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: Colors.blue,
          brightness: Brightness.dark,
        ),
        useMaterial3: true,
      ),
      initialRoute: initialRoute,
      routes: {
        '/onboarding': (context) => const OnboardingScreen(),
        '/main': (context) => const MainScreen(),
      },
    );
  }
}
