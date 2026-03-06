import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'presentation/screens/onboarding_screen.dart';
import 'presentation/screens/main_screen.dart';
import 'data/services/preference_service.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  
  // Check if onboarding is complete
  final prefs = PreferenceService();
  await prefs.init();
  final isOnboardingComplete = prefs.getOnboardingComplete();
  
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
