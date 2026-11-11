<template>
  <div class="match-edit" v-loading="loading">
    <header class="page-header">
      <div class="header-row">
        <el-button link :icon="ArrowLeft" class="back-btn" @click="$router.back()">返回</el-button>
        <span class="header-sep"></span>
        <div class="page-eyebrow">// CONTROL ROOM · MATCH</div>
        <span v-if="form" class="header-id">#{{ form.id }}</span>
      </div>
      <h1 class="page-title">编辑赛事</h1>
      <p class="page-sub">调整对阵、时间与状态，保存后立即对前台生效</p>
    </header>

    <div v-if="form" class="form-layout">
      <div class="form-col">
        <el-form ref="form" :model="form" :rules="rules" label-width="100px" label-position="top" class="edit-form">
          <section class="form-section">
            <div class="section-head">
              <span class="section-index">01</span>
              <h3 class="section-title">赛事信息</h3>
              <span class="section-meta">IDENTITY</span>
            </div>

            <div class="form-grid">
              <el-form-item label="赛事标题" prop="title" class="span-2">
                <el-input v-model="form.title" placeholder="例如: T1 vs Gen.G" />
              </el-form-item>

              <el-form-item label="联赛名称" prop="tournament">
                <el-input v-model="form.tournament" placeholder="例如: VCT Champions 2024" />
              </el-form-item>

              <el-form-item label="游戏" prop="gameId">
                <el-select v-model="form.gameId" placeholder="选择游戏" style="width: 100%">
                  <el-option
                    v-for="game in games"
                    :key="game.id"
                    :label="game.name"
                    :value="game.id"
                  />
                </el-select>
              </el-form-item>
            </div>
          </section>

          <section class="form-section">
            <div class="section-head">
              <span class="section-index">02</span>
              <h3 class="section-title">对阵安排</h3>
              <span class="section-meta">TEAMS</span>
            </div>

            <div class="form-grid">
              <el-form-item label="A队名称" prop="teamA">
                <el-input v-model="form.teamA" placeholder="A队名称" />
              </el-form-item>

              <el-form-item label="B队名称" prop="teamB">
                <el-input v-model="form.teamB" placeholder="B队名称" />
              </el-form-item>
            </div>
          </section>

          <section class="form-section">
            <div class="section-head">
              <span class="section-index">03</span>
              <h3 class="section-title">时间与状态</h3>
              <span class="section-meta">SCHEDULE</span>
            </div>

            <div class="form-grid">
              <el-form-item label="比赛时间" prop="matchTime">
                <el-date-picker
                  v-model="form.matchTime"
                  type="datetime"
                  placeholder="选择比赛时间"
                  value-format="YYYY-MM-DDTHH:mm:ss"
                  style="width: 100%"
                />
              </el-form-item>

              <el-form-item label="赛事状态">
                <el-select v-model="form.status" placeholder="选择状态" style="width: 100%">
                  <el-option :value="0" label="未开始" />
                  <el-option :value="1" label="进行中" />
                  <el-option :value="2" label="已结束" />
                </el-select>
              </el-form-item>
            </div>
          </section>

          <div class="action-bar">
            <div class="action-hint">
              <span class="action-dot"></span>保存后修改立即对前台生效
            </div>
            <div class="action-buttons">
              <el-button @click="$router.back()">取消</el-button>
              <el-button type="primary" class="submit-btn" :loading="submitLoading" @click="handleSubmit">保存修改</el-button>
            </div>
          </div>
        </el-form>
      </div>

      <aside class="preview-col">
        <div class="preview-panel u-panel">
          <div class="preview-head">
            <span class="preview-kicker">PREVIEW / 当前赛事</span>
            <span :class="['status-badge', statusBadgeClass(form.status)]">
              <span class="status-dot"></span>{{ statusLabel(form.status) }}
            </span>
          </div>

          <div class="preview-match">
            <div class="preview-team">
              <span class="preview-plate preview-plate--a">{{ (form.teamA || '?').slice(0, 1) }}</span>
              <span class="preview-name">{{ form.teamA || '待定' }}</span>
            </div>
            <span class="preview-vs">VS</span>
            <div class="preview-team preview-team--right">
              <span class="preview-plate preview-plate--b">{{ (form.teamB || '?').slice(0, 1) }}</span>
              <span class="preview-name">{{ form.teamB || '待定' }}</span>
            </div>
          </div>

          <div class="preview-title">{{ form.title || '未命名赛事' }}</div>

          <div class="preview-meta">
            <div class="meta-row">
              <span class="meta-key">TOURNAMENT</span>
              <span class="meta-val">{{ form.tournament || '待定' }}</span>
            </div>
            <div class="meta-row">
              <span class="meta-key">GAME</span>
              <span class="meta-val">{{ selectedGameName }}</span>
            </div>
            <div class="meta-row">
              <span class="meta-key">TIME</span>
              <span class="meta-val meta-val--mono">{{ formatPreviewTime(form.matchTime) }}</span>
            </div>
            <div class="meta-row">
              <span class="meta-key">MATCH ID</span>
              <span class="meta-val meta-val--mono">#{{ form.id }}</span>
            </div>
          </div>
        </div>

        <div class="tips-panel u-panel">
          <div class="preview-kicker">NOTICE / 操作提示</div>
          <ul class="tips-list">
            <li>状态改为「已结束」后，可在赛事管理中执行结算。</li>
            <li>离开页面前未保存的修改将丢失。</li>
          </ul>
        </div>
      </aside>
    </div>
  </div>
</template>

<script>
import { markRaw } from 'vue'
import request from '@/utils/request'
// 分页数据提取工具（兼容 records/list/content），统一提取自 @/utils/pagination
import { extractList } from '@/utils/pagination'
import { ArrowLeft } from '@element-plus/icons-vue'

export default {
  name: 'MatchEdit',
  data() {
    return {
      // 暴露图标组件供模板 :icon 绑定使用（markRaw 避免被响应式化）
      ArrowLeft: markRaw(ArrowLeft),
      loading: false,
      submitLoading: false,
      form: null,
      originalForm: null,
      games: [],
      rules: {
        title: [{ required: true, message: '请输入赛事标题', trigger: 'blur' }],
        teamA: [{ required: true, message: '请输入A队名称', trigger: 'blur' }],
        teamB: [{ required: true, message: '请输入B队名称', trigger: 'blur' }],
        matchTime: [{ required: true, message: '请选择比赛时间', trigger: 'change' }]
      }
    }
  },
  computed: {
    // 预览面板展示当前所选游戏名
    selectedGameName() {
      if (!this.form) return '未选择'
      var selected = this.games.find(item => item.id === this.form.gameId)
      return selected ? selected.name : '未选择'
    }
  },
  beforeRouteLeave(to, from, next) {
    if (this.hasUnsavedChanges()) {
      this.$confirm(
        '当前赛事信息已修改但尚未保存，确定要离开吗？未保存的更改将丢失。',
        '未保存的更改',
        {
          confirmButtonText: '离开',
          cancelButtonText: '继续编辑',
          type: 'warning',
          customClass: 'admin-confirm-warning'
        }
      ).then(() => {
        next()
      }).catch(() => {
        next(false)
      })
    } else {
      next()
    }
  },
  created() {
    this.fetchGames()
    this.fetchMatch()
  },
  methods: {
    fetchGames() {
      request.get('/game/list').then(res => {
        const data = res.data || res
        // 兼容 records/list/content 多种后端返回格式
        this.games = extractList(data)
      }).catch(() => {})
    },
    fetchMatch() {
      this.loading = true
      request.get('/match/' + this.$route.params.id).then(res => {
        const data = res.data || res
        this.form = {
          id: data.id,
          title: data.title || '',
          teamA: data.teamA || '',
          teamB: data.teamB || '',
          tournament: data.tournament || '',
          matchTime: data.matchTime || '',
          gameId: data.gameId || 1,
          status: data.status !== undefined ? data.status : 0
        }
        this.originalForm = JSON.parse(JSON.stringify(this.form))
      }).catch(() => {
        this.$message.error('赛事不存在')
        this.$router.back()
      }).finally(() => {
        this.loading = false
      })
    },
    // 预览用时间格式：ISO 的 T 换成空格，空值显示待定
    formatPreviewTime(timeStr) {
      if (!timeStr) return '待定'
      return String(timeStr).replace('T', ' ')
    },
    // 预览用状态文案
    statusLabel(status) {
      if (status === 1) return '进行中'
      if (status === 2) return '已结束'
      return '未开始'
    },
    // 预览用状态样式
    statusBadgeClass(status) {
      if (status === 1) return 'status-badge--live'
      if (status === 2) return 'status-badge--finished'
      return 'status-badge--upcoming'
    },
    hasUnsavedChanges() {
      if (!this.form || !this.originalForm) return false
      return JSON.stringify(this.form) !== JSON.stringify(this.originalForm)
    },
    handleSubmit() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.submitLoading = true
        request.put('/match/update', this.form).then(() => {
          this.originalForm = JSON.parse(JSON.stringify(this.form))
          this.$message.success('修改成功')
          this.$router.push('/admin')
        }).catch(() => {}).finally(() => {
          this.submitLoading = false
        })
      })
    }
  }
}
</script>

<style scoped>
.match-edit {
  position: relative;
}

/* ---------- Page header ---------- */
.page-header {
  position: relative;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-subtle);
}

.page-header::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: -1px;
  width: 44px;
  height: 2px;
  background: var(--brand);
  box-shadow: 0 0 12px var(--brand-line);
}

.header-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.header-row :deep(.el-button.back-btn) {
  height: 26px;
  padding: 0 8px 0 0;
  color: var(--text-tertiary);
  font-size: 12px;
  transition: color var(--duration-fast) ease;
}

.header-row :deep(.el-button.back-btn:hover) {
  color: var(--brand);
}

.header-sep {
  width: 1px;
  height: 12px;
  background: var(--border-default);
}

.page-eyebrow {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--brand);
}

.header-id {
  margin-left: auto;
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--text-faint);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  padding: 1px 7px;
}

.page-title {
  font-family: var(--font-body);
  font-size: 32px;
  font-weight: 800;
  line-height: 1.15;
  letter-spacing: -0.02em;
  color: var(--text-primary);
}

.page-sub {
  margin-top: 6px;
  font-size: 13px;
  color: var(--text-tertiary);
  letter-spacing: 0.02em;
}

/* ---------- Layout ---------- */
.form-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 24px;
  align-items: start;
}

.form-col {
  min-width: 0;
}

.edit-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ---------- Form sections ---------- */
.form-section {
  padding: 18px 20px 4px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.018) 0%, transparent 46%),
    var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
}

.section-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding-bottom: 12px;
  margin-bottom: 16px;
  border-bottom: 1px solid var(--border-subtle);
}

.section-index {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: var(--brand);
}

.section-title {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--text-primary);
}

.section-meta {
  margin-left: auto;
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--text-faint);
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 20px;
}

.span-2 {
  grid-column: 1 / -1;
}

.form-section :deep(.el-form-item) {
  margin-bottom: 16px;
}

.form-section :deep(.el-form-item__label) {
  padding-bottom: 5px !important;
  font-size: 11px !important;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--text-tertiary) !important;
  line-height: 1.4;
}

.form-section :deep(.el-form-item__content) {
  min-width: 0;
}

/* ---------- Action bar ---------- */
.action-bar {
  position: sticky;
  bottom: 16px;
  z-index: 5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 14px;
  background: var(--bg-surface-1);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
}

.action-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  font-size: 11.5px;
  color: var(--text-tertiary);
}

.action-dot {
  width: 5px;
  height: 5px;
  flex-shrink: 0;
  border-radius: 50%;
  background: var(--accent-primary);
  box-shadow: 0 0 8px var(--accent-primary);
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.action-buttons :deep(.el-button + .el-button) {
  margin-left: 0;
}

.submit-btn {
  min-width: 120px;
}

/* ---------- Preview column ---------- */
.preview-col {
  position: sticky;
  top: calc(var(--header-h) + 16px);
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.preview-panel {
  padding: 16px;
}

.preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-subtle);
}

.preview-kicker {
  font-family: var(--font-mono);
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--text-tertiary);
}

.preview-match {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: start;
  gap: 8px;
  padding: 18px 0 12px;
}

.preview-team {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  min-width: 0;
}

.preview-team--right {
  align-items: flex-end;
  text-align: right;
}

.preview-plate {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  font-family: var(--font-mono);
  font-size: 15px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: 0.02em;
  color: var(--text-primary);
  background: var(--bg-surface-2);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
}

.preview-plate--a {
  color: var(--brand);
  border-color: var(--brand-line);
}

.preview-plate--b {
  color: var(--accent-secondary);
}

.preview-name {
  max-width: 100%;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.35;
  word-break: break-word;
}

.preview-vs {
  padding-top: 12px;
  font-family: var(--font-display);
  font-size: 20px;
  letter-spacing: 0.06em;
  color: var(--text-faint);
}

.preview-title {
  padding: 10px 0 12px;
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.5;
  word-break: break-word;
  border-top: 1px solid var(--border-subtle);
}

.preview-meta {
  display: flex;
  flex-direction: column;
}

.meta-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 14px;
  padding: 7px 0;
  border-top: 1px solid var(--border-subtle);
}

.meta-key {
  flex-shrink: 0;
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.14em;
  color: var(--text-faint);
}

.meta-val {
  min-width: 0;
  font-size: 12px;
  color: var(--text-secondary);
  text-align: right;
  word-break: break-word;
}

.meta-val--mono {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 11px;
  letter-spacing: 0.01em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ---------- Tips ---------- */
.tips-panel {
  padding: 16px;
}

.tips-list {
  list-style: none;
  margin: 12px 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tips-list li {
  position: relative;
  padding-left: 12px;
  font-size: 11.5px;
  color: var(--text-tertiary);
  line-height: 1.55;
}

.tips-list li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 7px;
  width: 4px;
  height: 4px;
  background: var(--border-strong);
}

/* ---------- Responsive ---------- */
@media (max-width: 1024px) {
  .form-layout {
    grid-template-columns: minmax(0, 1fr) 300px;
    gap: 18px;
  }
}

@media (max-width: 860px) {
  .form-layout {
    grid-template-columns: 1fr;
  }

  .preview-col {
    position: static;
    flex-direction: row;
    flex-wrap: wrap;
  }

  .preview-panel,
  .tips-panel {
    flex: 1 1 260px;
  }
}

@media (max-width: 768px) {
  .page-title {
    font-size: 26px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-section {
    padding: 16px 14px 2px;
  }

  .action-bar {
    align-items: stretch;
    flex-direction: column;
    gap: 10px;
    bottom: 10px;
  }

  .action-buttons {
    width: 100%;
  }

  .action-buttons :deep(.el-button) {
    flex: 1;
    min-height: 42px;
  }

  .preview-col {
    flex-direction: column;
  }
}

@media (max-width: 480px) {
  .page-title {
    font-size: 23px;
  }

  .preview-match {
    padding-top: 14px;
  }

  .preview-name {
    font-size: 12px;
  }
}
</style>
