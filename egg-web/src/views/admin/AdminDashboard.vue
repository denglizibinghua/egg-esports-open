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

        <el-table :data="matches" style="width: 100%" v-loading="loading" empty-text="暂无赛事">
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
              <el-tag v-if="scope.row.status === 0" type="warning" size="small">未开始</el-tag>
              <el-tag v-else-if="scope.row.status === 1" type="danger" size="small">进行中</el-tag>
              <el-tag v-else-if="scope.row.status === 2" type="success" size="small">已结束</el-tag>
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
</style>