import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue'),
    meta: { title: '工位总览' }
  },
  {
    path: '/stopper',
    name: 'Stopper',
    component: () => import('@/views/StopperList.vue'),
    meta: { title: '挡块档案' }
  },
  {
    path: '/device-stopper',
    name: 'DeviceStopper',
    component: () => import('@/views/DeviceStopperView.vue'),
    meta: { title: '适配设备视图' }
  },
  {
    path: '/shift',
    name: 'Shift',
    component: () => import('@/views/ShiftList.vue'),
    meta: { title: '移位登记' }
  },
  {
    path: '/inventory',
    name: 'Inventory',
    component: () => import('@/views/InventoryList.vue'),
    meta: { title: '月度清点' }
  },
  {
    path: '/inventory/detail/:id',
    name: 'InventoryDetail',
    component: () => import('@/views/InventoryDetail.vue'),
    meta: { title: '盘点明细' }
  },
  {
    path: '/scrap',
    name: 'Scrap',
    component: () => import('@/views/ScrapList.vue'),
    meta: { title: '报废归档' }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 限位挡块资产登记系统` : '限位挡块资产登记系统'
  next()
})

export default router
