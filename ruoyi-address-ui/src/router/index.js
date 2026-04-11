import Vue from 'vue';
import Router from 'vue-router';

import { registerAuthGuard } from './auth-guard';
import Login from '../views/Login.vue';
import StandardDetail from '../views/StandardDetail.vue';
import StandardList from '../views/StandardList.vue';
import StandardMerge from '../views/StandardMerge.vue';
import StandardSplit from '../views/StandardSplit.vue';
import StandardApprovalRecords from '../views/StandardApprovalRecords.vue';
import ImportRecords from '../views/ImportRecords.vue';
import OperationLogs from '../views/OperationLogs.vue';
import InstallationList from '../views/InstallationList.vue';
import SelectionTools from '../views/SelectionTools.vue';
import AddressLabels from '../views/AddressLabels.vue';
import ManagementStation from '../views/ManagementStation.vue';
import MonitorRecords from '../views/MonitorRecords.vue';
import MonitorRules from '../views/MonitorRules.vue';
import MonitorTask from '../views/MonitorTask.vue';
import SearchOpsConsole from '../views/SearchOpsConsole.vue';

Vue.use(Router);

const router = new Router({
  mode: 'history',
  routes: [
    {
      path: '/login',
      component: Login,
      meta: {
        public: true,
        bareLayout: true
      }
    },
    { path: '/', redirect: '/standard/list' },
    { path: '/standard/list', component: StandardList },
    { path: '/standard/detail/:segmId', component: StandardDetail },
    { path: '/standard/approvals', component: StandardApprovalRecords },
    { path: '/standard/merge', component: StandardMerge },
    { path: '/standard/split', component: StandardSplit },
    { path: '/import/records', component: ImportRecords },
    { path: '/operation/logs', component: OperationLogs },
    { path: '/installation/list', component: InstallationList },
    { path: '/selection/tools', component: SelectionTools },
    { path: '/standard/labels', component: AddressLabels },
    { path: '/management/station', component: ManagementStation },
    { path: '/monitor/records', component: MonitorRecords },
    { path: '/monitor/rules', component: MonitorRules },
    { path: '/monitor/task', component: MonitorTask },
    { path: '/ops/search', component: SearchOpsConsole }
  ]
});

registerAuthGuard(router);

export default router;
