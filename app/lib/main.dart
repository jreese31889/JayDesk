import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import 'package:jaydesk/theme/jay_theme.dart';
import 'package:jaydesk/state/app_state.dart';
import 'package:jaydesk/services/platform_bridge.dart';
import 'package:jaydesk/screens/welcome_screen.dart';
import 'package:jaydesk/screens/home_screen.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  JayDeskPlatform.init();

  runApp(
    ChangeNotifierProvider(
      create: (_) => AppState(),
      child: const JayDeskApp(),
    ),
  );
}

class JayDeskApp extends StatefulWidget {
  const JayDeskApp({super.key});

  @override
  State<JayDeskApp> createState() => _JayDeskAppState();
}

class _JayDeskAppState extends State<JayDeskApp> {
  @override
  void initState() {
    super.initState();
    SystemChrome.setEnabledSystemUIMode(
      SystemUiMode.manual,
      overlays: SystemUiOverlay.values,
    );
    // Initialize platform bridge and load state
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<AppState>().initialize();
    });
  }

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    final isDark = state.isDarkMode;

    final overlayStyle = SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      statusBarIconBrightness: isDark ? Brightness.light : Brightness.dark,
      statusBarBrightness: isDark ? Brightness.dark : Brightness.light,
      systemNavigationBarColor: Colors.transparent,
      systemNavigationBarIconBrightness: isDark ? Brightness.light : Brightness.dark,
    );

    SystemChrome.setSystemUIOverlayStyle(overlayStyle);

    return MaterialApp(
      title: 'JayDesk',
      debugShowCheckedModeBanner: false,
      theme: DroidTheme.lightThemeData,
      darkTheme: DroidTheme.darkThemeData,
      themeMode: state.themeMode,
      builder: (context, child) {
        return AnnotatedRegion<SystemUiOverlayStyle>(
          value: overlayStyle,
          child: child ?? const SizedBox.shrink(),
        );
      },
      home: state.isSetupComplete ? const HomeScreen() : const WelcomeScreen(),
    );
  }
}
