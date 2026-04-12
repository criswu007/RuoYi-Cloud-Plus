import { describe, expect, it } from 'vitest';
import App from './App.vue';

describe('应用导航', () => {
  it('应提供 ES 运维页标题映射', () => {
    const state = App.data();

    expect(state.titleMap['/ops/search']).toBe('ES 运维');
  });

  it('应提供标准地址工单页标题映射', () => {
    const state = App.data();

    expect(state.titleMap['/monitor/work-orders']).toBe('标准地址工单');
  });

  it('不应再提供刷新 Token 的页面方法', () => {
    expect(App.methods.refreshToken).toBeUndefined();
  });

  it('登录页命中 bareLayout 时应隐藏业务框架布局', () => {
    expect(App.computed.useBareLayout.call({
      $route: {
        matched: [{ meta: { bareLayout: true } }]
      }
    })).toBe(true);
  });
});
