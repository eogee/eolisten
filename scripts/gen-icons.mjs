/**
 * 生成安卓图标源素材（assets/）：罗兰紫底 + 白色号角前景。
 * 原始 logo（eolisten/logo/final/eolisten-logo-violet.svg）是紫底白底互换前的紫图形，
 * 紫底上放紫图形会隐形，故前景统一转白色；viewBox 裁到图形包围盒保证居中。
 * 运行：node scripts/gen-icons.mjs（sharp 由 @capacitor/assets 依赖提供）
 */
import sharp from 'sharp';

const MARK = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="28.5 18.2 53.3 53.3">
  <g>
    <path fill="#ffffff" stroke="#ffffff" stroke-width="3" stroke-linejoin="round"
          fill-rule="evenodd"
          d="M 30 70 L 70 70 A 40 40 0 0 0 30 30 Z M 40 55 A 5 5 0 1 0 40 65 A 5 5 0 1 0 40 55 Z"/>
  </g>
  <g fill="none" stroke="#ffffff" stroke-width="3" stroke-linecap="round">
    <path d="M 46.4 24.9 A 48 48 0 0 1 75.1 53.6"/>
    <path d="M 56.8 19.7 A 57 57 0 0 1 80.3 43.3"/>
  </g>
</svg>`;

const markPng = (size) =>
  sharp(Buffer.from(MARK)).resize(size, size).png().toBuffer();

// 自适应图标前景：透明底，图形收在 66/108 安全区内（约 58%）
const FG = 594; // 1024 * 0.58
await sharp({
  create: { width: 1024, height: 1024, channels: 4, background: { r: 0, g: 0, b: 0, alpha: 0 } },
})
  .composite([{ input: await markPng(FG), top: Math.round((1024 - FG) / 2), left: Math.round((1024 - FG) / 2) }])
  .png()
  .toFile('assets/icon-foreground.png');

// 自适应图标背景：纯罗兰紫
await sharp({
  create: { width: 1024, height: 1024, channels: 4, background: '#8b5cf6' },
})
  .png()
  .toFile('assets/icon-background.png');

// 方形整图（旧版启动器 / 商店）：罗兰紫底 + 72% 白图形
const ONLY = 737;
await sharp({
  create: { width: 1024, height: 1024, channels: 4, background: '#8b5cf6' },
})
  .composite([{ input: await markPng(ONLY), top: Math.round((1024 - ONLY) / 2), left: Math.round((1024 - ONLY) / 2) }])
  .png()
  .toFile('assets/icon-only.png');

console.log('icons generated');
