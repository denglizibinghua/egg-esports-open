import { defineStore } from 'pinia'
import request from '@/utils/request'
import type { ApiResponse } from '@/types'

interface LoanStatusResponse {
  loanAmount: number
  loanLimit: number
  lastStipend: string
}

interface LoanState {
  loanAmount: number
  loanLimit: number
  lastStipend: string
  loading: boolean
}

export const useLoanStore = defineStore('loan', {
  state: (): LoanState => ({
    loanAmount: 0,
    loanLimit: 0,
    lastStipend: '',
    loading: false
  }),
  actions: {
    async fetchStatus(): Promise<void> {
      this.loading = true
      try {
        const res = await request.get<unknown, ApiResponse<LoanStatusResponse>>('/loan/status')
        const data = res.data
        this.loanAmount = data.loanAmount
        this.loanLimit = data.loanLimit
        this.lastStipend = data.lastStipend
      } finally {
        this.loading = false
      }
    },
    async applyStipend(): Promise<void> {
      await request.post<unknown, ApiResponse<unknown>>('/loan/stipend')
      await this.fetchStatus()
    },
    async repay(amount: number): Promise<void> {
      await request.post<unknown, ApiResponse<unknown>>('/loan/repay', { amount })
      await this.fetchStatus()
    },
    async applyRelief(): Promise<void> {
      await request.post<unknown, ApiResponse<unknown>>('/loan/relief')
      await this.fetchStatus()
    }
  }
})