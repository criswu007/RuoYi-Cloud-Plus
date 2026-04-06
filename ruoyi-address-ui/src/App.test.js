import { describe, expect, it } from 'vitest';
import App from './App.vue';

describe('应用导航', () => {
  it('应提供 ES 运维页标题映射', () => {
    const state = App.data();

    expect(state.titleMap['/ops/search']).toBe('ES 运维');
  });
});
