import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.eogee.eolisten',
  appName: 'EoListen',
  webDir: 'www',
  server: {
    // 远程加载壳：业务全部留在 eogee.com，UI 更新即时生效（见 eolisten/doc/安卓客户端方案.md）
    url: 'https://eogee.com/listen/',
    allowNavigation: ['eogee.com'],
    androidScheme: 'https',
  },
  plugins: {
    SplashScreen: {
      // 冷启动只走系统 splash（Android 12+ 用应用图标），不叠自定义层
      launchShowDuration: 0,
    },
  },
};

export default config;
