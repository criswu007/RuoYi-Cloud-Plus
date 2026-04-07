import { describe, expect, it } from 'vitest';
import router from './index';

describe('标准地址路由', () => {
  it('应提供标准地址详情页路由', () => {
    const detailRoute = router.options.routes.find(route => route.path === '/standard/detail/:segmId');

    expect(detailRoute).toBeTruthy();
  });

  it('应提供管理站管理页路由', () => {
    const stationRoute = router.options.routes.find(route => route.path === '/management/station');

    expect(stationRoute).toBeTruthy();
  });

  it('应提供 ES 运维页路由', () => {
    const route = router.options.routes.find(item => item.path === '/ops/search');

    expect(route).toBeTruthy();
  });

  it('应提供标准地址审批管理页路由', () => {
    const route = router.options.routes.find(item => item.path === '/standard/approvals');

    expect(route).toBeTruthy();
  });
});
