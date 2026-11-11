<template>
  <div class="match-create">
    <header class="page-header">
      <div class="header-row">
        <el-button link :icon="ArrowLeft" class="back-btn" @click="$router.back()">返回</el-button>
        <span class="header-sep"></span>
        <div class="page-eyebrow">// CONTROL ROOM · MATCH</div>
      </div>
      <h1 class="page-title">创建赛事</h1>
      <p class="page-sub">填写对阵与时间，创建后立即对前台开放</p>
    </header>

    <div class="form-layout">
      <div class="form-col">
        <el-form ref="form" :model="form" :rules="rules" label-width="100px" label-position="top" class="create-form">
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
              <h3 class="section-title">比赛时间</h3>
              <span class="section-meta">SCHEDULE</span>
            </div>

            <div class="form-grid">
              <el-form-item label="比赛时间" prop="matchTime" class="span-2">
                <el-date-picker
                  v-model="form.matchTime"
                  type="datetime"
                  placeholder="选择比赛时间"
                  value-format="YYYY-MM-DDTHH:mm:ss"
                  :picker-options="pickerOptions"
                  style="width: 100%"
                />
              </el-form-item>
            </div>
          </section>

          <div class="action-bar">
            <div class="action-hint">
              <span class="action-dot"></span>创建后赛事立即对所有用户可见
            </div>
            <div class="action-buttons">
              <el-button @click="$router.back()">取消</el-button>
              <el-button type="primary" class="submit-btn" :loading="submitLoading" @click="handleSubmit">创建赛事</el-button>
            </div>
          </div>
        </el-form>
      </div>

      <aside class="preview-col">
        <div class="preview-panel u-panel">
          <div class="preview-head">
            <span class="preview-kicker">PREVIEW / 实时预览</span>
            <span class="preview-state">DRAFT</span>
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
          </div>
        </div>

        <div class="checklist-panel u-panel">
          <div class="preview-kicker">CHECKLIST / 发布前确认</div>
          <ul class="checklist">
            <li :class="{ 'is-done': form.title.trim() }"><span class="check-box"></span>赛事标题已填写</li>
            <li :class="{ 'is-done': form.teamA.trim() && form.teamB.trim() }"><span class="check-box"></span>双方战队已填写</li>
            <li :class="{ 'is-done': form.matchTime }"><span class="check-box"></span>比赛时间已选择</li>
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
  name: 'MatchCreate',
  data() {
    return {
      // 暴露图标组件供模板 :icon 绑定使用（markRaw 避免被响应式化）
      ArrowLeft: markRaw(ArrowLeft),
      form: {
        title: '',
        teamA: '',
        teamB: '',
        tournament: '',
        matchTime: '',
        gameId: 1
      },
      games: [],
      rules: {
        title: [{ required: true, message: '请输入赛事标题', trigger: 'blur' }],
        teamA: [{ required: true, message: '请输入A队名称', trigger: 'blur' }],
        teamB: [{ required: true, message: '请输入B队名称', trigger: 'blur' }],
        matchTime: [{ required: true, message: '请选择比赛时间', trigger: 'change' }]
      },
      submitLoading: false,
      pickerOptions: {
        disabledDate(time) {
          return time.getTime() < Date.now() - 86400000
        }
      }
    }
  },
  computed: {
    // 预览面板展示当前所选游戏名
    selectedGameName() {
      var selected = this.games.find(item => item.id === this.form.gameId)
      return selected ? selected.name : '未选择'
    }
  },
  created() {
    this.fetchGames()
  },
  methods: {
    fetchGames() {
      request.get('/game/list').then(res => {
        const data = res.data || res
        // 兼容 records/list/content 多种后端返回格式
        this.games = extractList(data)
        if (this.games.length > 0 && (this.form.gameId === 1 || !this.form.gameId)) {
          this.form.gameId = this.games[0].id
        }
      }).catch(() => {})
    },
    // 预览用时间格式：ISO 的 T 换成空格，空值显示待定
    formatPreviewTime(timeStr) {
      if (!timeStr) return '待定'
      return String(timeStr).replace('T', ' ')
    },
    handleSubmit() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.$confirm(
          '确认创建赛事「' + this.form.title + '」？创建后赛事将对所有用户可见。',
          '确认创建',
          {
            confirmButtonText: '确认创建',
            cancelButtonText: '取消',
            type: 'info',
            customClass: 'admin-confirm-warning'
          }
        ).then(() => {
          this.submitLoading = true
          request.post('/match/create', this.form).then(() => {
            this.$message.success('赛事创建成功')
            this.$router.push('/admin')
          }).catch(() => {}).finally(() => {
            this.submitLoading = false
          })
        }).catch(() => {})
      })
    }
  }
}
</script>

<style scoped>
.match-create {
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

.create-form {
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

.form-section :deep(.el-date-editor) {
  --el-date-editor-width: 100%;
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

.preview-state {
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.14em;
  color: var(--accent-warning);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  padding: 1px 6px;
  background: var(--bg-surface-2);
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

/* ---------- Checklist ---------- */
.checklist-panel {
  padding: 16px;
}

.checklist {
  list-style: none;
  margin: 12px 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 9px;
}

.checklist li {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 12px;
  color: var(--text-tertiary);
}

.check-box {
  position: relative;
  flex-shrink: 0;
  width: 12px;
  height: 12px;
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-sharp);
  transition: all var(--duration-fast) ease;
}

.checklist li.is-done {
  color: var(--text-secondary);
}

.checklist li.is-done .check-box {
  border-color: var(--accent-primary);
  background: var(--accent-primary-soft);
}

.checklist li.is-done .check-box::after {
  content: '';
  position: absolute;
  left: 3px;
  top: 1px;
  width: 4px;
  height: 7px;
  border-right: 1.5px solid var(--accent-primary);
  border-bottom: 1.5px solid var(--accent-primary);
  transform: rotate(40deg);
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
  .checklist-panel {
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
