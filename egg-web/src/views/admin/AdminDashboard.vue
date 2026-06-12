<template>
  <div class="admin-dashboard">
    <div class="page-header">
      <h2 class="page-title">管理后台</h2>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="赛事管理" name="match">
        <div class="tab-header">
          <el-radio-group v-model="matchStatus" size="small" @change="fetchMatches">
            <el-radio-button label="all">全部</el-radio-button>
            <el-radio-button label="0">未开始</el-radio-button>
            <el-radio-button label="1">进行中</el-radio-button>
            <el-radio-button label="2">已结束</el-radio-button>
          </el-radio-group>
          <el-button type="primary" size="small" icon="el-icon-plus" @click="$router.push('/admin/match/create')">添加赛事</el-button>
        </div>

        <!-- Loading Skeleton -->
        <div v-if="loading" class="skeleton-table">
          <div class="skeleton-table-header">
            <div class="skeleton-line skeleton-line--cell-sm"></div>
            <div class="skeleton-line skeleton-line--cell-lg"></div>
            <div class="skeleton-line skeleton-line--cell-md"></div>
            <div class="skeleton-line skeleton-line--cell-md"></div>
            <div class="skeleton-line skeleton-line--cell-md"></div>
            <div class="skeleton-line skeleton-line--cell-md"></div>
            <div class="skeleton-line skeleton-line--cell-sm"></div>
            <div class="skeleton-line skeleton-line--cell-sm"></div>
          </div>
          <div v-for="i in 5" :key="'admin-skel-'+i" class="skeleton-table-row">
            <div class="skeleton-line skeleton-line--cell-sm"></div>
            <div class="skeleton-line skeleton-line--cell-lg"></div>
            <div class="skeleton-line skeleton-line--cell-md"></div>
            <div class="skeleton-line skeleton-line--cell-md"></div>
            <div class="skeleton-line skeleton-line--cell-md"></div>
            <div class="skeleton-line skeleton-line--cell-md"></div>
            <div class="skeleton-line skeleton-line--cell-sm"></div>
            <div class="skeleton-line skeleton-line--cell-sm"></div>
          </div>
        </div>

        <!-- Empty State -->
        <div v-else-if="matches.length === 0" class="empty-state">
          <div class="empty-icon">📝</div>
          <h3 class="empty-title">还没有赛事</h3>
          <p class="empty-desc">创建第一场比赛，开始你的竞猜平台</p>
          <el-button type="primary" class="empty-action" @click="$router.push('/admin/match/create')">创建赛事</el-button>
        </div>

        <!-- Match Table -->
        <template v-else>
          <el-table :data="matches" style="width: 100%">
            <el-table-column label="ID" width="70" prop="id" />
            <el-table-column label="标题" min-width="150" prop="title" />
            <el-table-column label="A队" width="120" prop="teamA" />
            <el-table-column label="B队" width="120" prop="teamB" />
            <el-table-column label="联赛" width="120" prop="tournament" />
            <el-table-column label="时间" width="160">
              <template slot-scope="scope">
                {{ formatDate(scope.row.matchTime) }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template slot-scope="scope">
                <span v-if="scope.row.status === 0" class="status-badge status-badge--upcoming">
                  <span class="status-dot"></span>未开始
                </span>
                <span v-else-if="scope.row.status === 1" class="status-badge status-badge--live">
                  <span class="status-dot status-dot--pulse"></span>进行中
                </span>
                <span v-else-if="scope.row.status === 2" class="status-badge status-badge--finished">已结束</span>
              </template>
            </el-table-column>
            <el-table-column label="结果" width="100" align="center">
              <template slot-scope="scope">
                <template v-if="scope.row.status === 2">
                  <template v-if="scope.row.result === 0">A胜</template>
                  <template v-else-if="scope.row.result === 1">B胜</template>
                  <template v-else-if="scope.row.result === 2">平局</template>
                  <template v-else>未结算</template>
                </template>
                <template v-else>-</template>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" align="center">
              <template slot-scope="scope">
                <el-button type="text" size="small" @click="$router.push('/admin/match/' + scope.row.id)">编辑</el-button>
                <el-button
                  v-if="scope.row.status === 2 && (scope.row.result === null || scope.row.result === undefined)"
                  type="text"
                  size="small"
                  class="settle-btn"
                  @click="openSettleDialog(scope.row)"
                >
                  结算
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>

        <el-pagination
          v-if="total > 0"
          background
          layout="prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :current-page.sync="currentPage"
          @current-change="fetchMatches"
        />
      </el-tab-pane>
    </el-tabs>

    <!-- Settle Dialog -->
    <el-dialog title="赛事结算" :visible.sync="settleDialogVisible" width="400px" custom-class="settle-dialog">
      <div class="settle-content">
        <p class="settle-match-name">{{ settleMatch ? settleMatch.teamA + ' VS ' + settleMatch.teamB : '' }}</p>
        <p class="settle-label">选择比赛结果：</p>
        <el-radio-group v-model="settleResult">
          <el-radio :label="0">{{ settleMatch ? settleMatch.teamA : 'A队' }} 获胜</el-radio>
          <el-radio :label="1">{{ settleMatch ? settleMatch.teamB : 'B队' }} 获胜</el-radio>
          <el-radio :label="2">平局</el-radio>
        </el-radio-group>
      </div>
      <span slot="footer">
        <el-button @click="settleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="settleLoading" @click="handleSettle">确认结算</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'AdminDashboard',
  data() {
    return {
      activeTab: 'match',
      matches: [],
      loading: false,
      matchStatus: 'all',
      currentPage: 1,
      pageSize: 20,
      total: 0,
      settleDialogVisible: false,
      settleMatch: null,
      settleResult: null,
      settleLoading: false
    }
  },
  created() {
    this.fetchMatches()
  },
  methods: {
    fetchMatches() {
      this.loading = true
      const params = {
        page: this.currentPage,
        size: this.pageSize
      }
      if (this.matchStatus !== 'all') {
        params.status = this.matchStatus
      }
      request.get('/match/list', { params }).then(res => {
        const data = res.data || res
        if (Array.isArray(data)) {
          this.matches = data
          this.total = data.length
        } else if (data.records) {
          this.matches = data.records
          this.total = data.total || data.records.length
        } else {
          this.matches = data.list || data.content || []
          this.total = data.total || data.totalElements || 0
        }
      }).catch(() => {}).finally(() => {
        this.loading = false
      })
    },
    openSettleDialog(match) {
      this.settleMatch = match
      this.settleResult = null
      this.settleDialogVisible = true
    },
    handleSettle() {
      if (this.settleResult === null) {
        this.$message.warning('请选择比赛结果')
        return
      }
      this.settleLoading = true
      request.put('/match/settle', {
        matchId: this.settleMatch.id,
        result: this.settleResult
      }).then(() => {
        this.$message.success('结算成功')
        this.settleDialogVisible = false
        this.fetchMatches()
      }).catch(() => {}).finally(() => {
        this.settleLoading = false
      })
    },
    formatDate(timeStr) {
      if (!timeStr) return '-'
      const d = new Date(timeStr)
      const y = d.getFullYear()
      const m = (d.getMonth() + 1).toString().padStart(2, '0')
      const day = d.getDate().toString().padStart(2, '0')
      const h = d.getHours().toString().padStart(2, '0')
      const min = d.getMinutes().toString().padStart(2, '0')
      return y + '-' + m + '-' + day + ' ' + h + ':' + min
    }
  }
}
</script>

<style scoped>
.page-header {
  margin-bottom: 24px;
}

.page-title {
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 32px;
  letter-spacing: 1px;
}

.tab-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.settle-btn {
  color: var(--accent-primary) !important;
  font-weight: 600;
}

.settle-content {
  text-align: center;
}

.settle-match-name {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 16px;
}

.settle-label {
  color: var(--text-secondary);
  margin-bottom: 12px;
}

.settle-content .el-radio {
  display: block;
  margin-bottom: 12px;
  color: var(--text-primary);
}

/* Empty State */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 24px;
  text-align: center;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.6;
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.empty-desc {
  font-size: 14px;
  color: var(--text-tertiary);
  margin-bottom: 24px;
  max-width: 300px;
}

.empty-action {
  background: var(--accent-primary) !important;
  border-color: var(--accent-primary) !important;
  color: #0a0a0f !important;
  font-weight: 600;
  border-radius: var(--radius-md) !important;
}

/* Skeleton Loading */
@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

.skeleton-table {
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.skeleton-table-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 2px solid var(--border-subtle);
}

.skeleton-table-header .skeleton-line {
  background: linear-gradient(90deg, var(--bg-surface-3) 25%, var(--bg-surface-2) 50%, var(--bg-surface-3) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
  border-radius: var(--radius-sm);
  height: 14px;
}

.skeleton-table-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--border-subtle);
}

.skeleton-table-row:last-child {
  border-bottom: none;
}

.skeleton-table-row .skeleton-line {
  background: linear-gradient(90deg, var(--bg-surface-2) 25%, var(--bg-surface-3) 50%, var(--bg-surface-2) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
  border-radius: var(--radius-sm);
  height: 14px;
}

.skeleton-line--cell-sm { width: 50px; flex-shrink: 0; }
.skeleton-line--cell-md { width: 90px; flex-shrink: 0; }
.skeleton-line--cell-lg { flex: 1; min-width: 100px; }
</style>