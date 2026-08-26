import { createRouter, createWebHistory } from 'vue-router'
// @ts-expect-error: Vue SFC module typing is provided by project shim; suppress local import type error.
import Transferform from '../views/Transferform.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: Transferform,
    },
  ],
})

export default router
