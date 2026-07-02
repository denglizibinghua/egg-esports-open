<template>
  <div class="admin-dashboard">
    <div class="page-header">
      <h2 class="page-title">管理后台</h2>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="赛事管理" name="match">
        <div class="tab-header">
          <el-radio-group v-model="matchStatus" size="small" @change="fetchMatches">
            <el-radio-button value="all">全部</el-radio-button>
            <el-radio-button value="0">未开始</el-radio-button>
            <el-radio-button value="1">进行中</el-radio-button>
            <el-radio-button value="2">已结束</el-radio-button>
          </el-radio-group>
          <el-button type="primary" size="small" :icon="Plus" @click="$router.push('/admin/match/create')">添加赛事</el-button>
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
              <template #default="scope">
                {{ formatDate(scope.row.matchTime) }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="scope">
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
              <template #default="scope">
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
              <template #default="scope">
                <el-button link size="small" @click="$router.push('/admin/match/' + scope.row.id)">编辑</el-button>
                <el-button
                  v-if="scope.row.status === 2 && (scope.row.result === null || scope.row.result === undefined)"
                  link
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
          v-model:current-page="currentPage"
          @current-change="fetchMatches"
        />
      </el-tab-pane>

      <el-tab-pane label="批量导入" name="import">
        <div class="import-panel">
          <div class="import-instructions">
            <h4 class="import-section-title">JSON 数据格式</h4>
            <p class="import-hint">粘贴 JSON 数组，每个对象需包含：title, teamA, teamB, matchTime。可选字段：gameId, teamALogo, teamBLogo, tournament, status</p>
          </div>

          <el-input
            v-model="importJsonText"
            type="textarea"
            :rows="8"
            placeholder='[{"title":"LPL春季赛决赛","teamA":"TES","teamB":"JDG","matchTime":"2026-06-15 18:00:00","tournament":"LPL","status":0}]'
            class="import-textarea"
          />

          <div class="import-actions">
            <el-button type="primary" size="small" :disabled="!importJsonText.trim()" @click="parseImportJson">解析预览</el-button>
            <el-button size="small" @click="clearImport">清空</el-button>
          </div>

          <div v-if="importParseError" class="import-parse-error">
            <el-alert :title="importParseError" type="error" :closable="false" show-icon />
          </div>

          <template v-if="importParsedList.length > 0">
            <div class="import-preview-header">
              <span class="import-preview-count">已解析 {{ importParsedList.length }} 条赛事</span>
            </div>

            <el-table :data="importParsedList" style="width: 100%" max-height="360" size="small">
              <el-table-column label="序号" width="60" align="center">
                <template #default="scope">{{ scope.$index + 1 }}</template>
              </el-table-column>
              <el-table-column label="标题" min-width="150" prop="title" />
              <el-table-column label="A队" width="120" prop="teamA" />
              <el-table-column label="B队" width="120" prop="teamB" />
              <el-table-column label="联赛" width="120" prop="tournament">
                <template #default="scope">{{ scope.row.tournament || '-' }}</template>
              </el-table-column>
              <el-table-column label="比赛时间" width="160">
                <template #default="scope">{{ formatDate(scope.row.matchTime) }}</template>
              </el-table-column>
              <el-table-column label="状态" width="100" align="center">
                <template #default="scope">
                  <span :class="['status-badge', statusBadgeClass(scope.row.status)]">
                    <span class="status-dot"></span>{{ statusLabel(scope.row.status) }}
                  </span>
                </template>
              </el-table-column>
            </el-table>

            <div class="import-submit-row">
              <el-button type="primary" :loading="importSubmitting" @click="submitBatchImport">确认导入 {{ importParsedList.length }} 条</el-button>
            </div>
          </template>

          <div v-if="importResult" class="import-result">
            <el-alert
              :title="'成功导入 ' + importResult.success + ' 条，失败 ' + importResult.fail + ' 条'"
              :type="importResult.fail > 0 ? 'warning' : 'success'"
              :closable="false"
              show-icon
            />
            <div v-if="importResult.errors && importResult.errors.length > 0" class="import-errors">
              <div class="import-errors-toggle" @click="importErrorsExpanded = !importErrorsExpanded">
                <el-icon><component :is="importErrorsExpanded ? 'ArrowDown' : 'ArrowRight'" /></el-icon>
                <span>查看失败详情 ({{ importResult.errors.length }} 条)</span>
              </div>
              <div v-if="importErrorsExpanded" class="import-errors-list">
                <div v-for="(err, idx) in importResult.errors" :key="'err-'+idx" class="import-error-item">
                  <span class="import-error-index">#{{ err.index + 1 }}</span>
                  <span class="import-error-title">{{ err.title || '未命名' }}</span>
                  <span class="import-error-msg">{{ err.error }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="数据同步" name="sync">
        <div class="sync-panel">
          <div class="sync-action-area">
            <el-button
              type="primary"
              :loading="syncLoading"
              :disabled="syncLoading"
              :icon="Refresh"
              @click="triggerSync"
            >
              {{ syncLoading ? '正在同步...' : '同步全部赛事数据（Riot + PandaScore + Liquipedia + GRID）' }}
            </el-button>
            <p v-if="lastSyncTime" class="sync-last-time">
              上次同步: {{ formatSyncTime(lastSyncTime) }}
            </p>
          </div>

          <!-- Loading Skeleton -->
          <div v-if="syncLoading" class="sync-skeleton">
            <div class="sync-skeleton-grid">
              <div v-for="i in 6" :key="'sync-skel-'+i" class="sync-skeleton-card">
                <div class="skeleton-line skeleton-line--stat-label"></div>
                <div class="skeleton-line skeleton-line--stat-value"></div>
              </div>
            </div>
          </div>

          <!-- Results Summary -->
          <div v-if="syncResult && !syncLoading" class="sync-results">
            <div class="sync-stats-grid">
              <div class="sync-stat-card">
                <span class="sync-stat-value sync-stat-value--gray">{{ syncResult.totalFetched }}</span>
                <span class="sync-stat-label">获取赛事</span>
              </div>
              <div class="sync-stat-card">
                <span class="sync-stat-value sync-stat-value--gray">{{ syncResult.totalSkipped }}</span>
                <span class="sync-stat-label">跳过(历史数据)</span>
              </div>
              <div class="sync-stat-card">
                <span class="sync-stat-value sync-stat-value--green">{{ syncResult.totalCreated }}</span>
                <span class="sync-stat-label">新增</span>
              </div>
              <div class="sync-stat-card">
                <span class="sync-stat-value sync-stat-value--blue">{{ syncResult.totalUpdated }}</span>
                <span class="sync-stat-label">更新</span>
              </div>
            </div>

            <!-- Per-Game Stats -->
            <div v-if="syncResult.perGameStats" class="sync-per-game">
              <div v-for="(stats, gameKey) in syncResult.perGameStats" :key="gameKey" class="sync-per-game-item">
                <span class="sync-per-game-label">{{ getGameNameByKey(gameKey) }}:</span>
                <span class="sync-per-game-val">+{{ stats[0] }} ~{{ stats[1] }} -{{ stats[2] }}</span>
              </div>
            </div>

            <!-- Error Display -->
            <div v-if="syncResult.errors && syncResult.errors.length > 0" class="sync-errors">
              <div class="sync-errors-title">同步错误</div>
              <div class="sync-errors-list">
                <div v-for="(err, idx) in syncResult.errors" :key="'sync-err-'+idx" class="sync-error-item">
                  {{ err }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="公告管理" name="announcement">
        <div class="tab-header">
          <span class="announcement-tab-title">公告列表</span>
          <el-button type="primary" size="small" :icon="Plus" @click="openAnnouncementDialog()">发布公告</el-button>
        </div>

        <div v-if="announcementLoading" class="skeleton-table">
          <div class="skeleton-table-header">
            <div class="skeleton-line skeleton-line--cell-lg"></div>
            <div class="skeleton-line skeleton-line--cell-md"></div>
            <div class="skeleton-line skeleton-line--cell-md"></div>
            <div class="skeleton-line skeleton-line--cell-md"></div>
            <div class="skeleton-line skeleton-line--cell-sm"></div>
          </div>
          <div v-for="i in 4" :key="'ann-skel-'+i" class="skeleton-table-row">
            <div class="skeleton-line skeleton-line--cell-lg"></div>
            <div class="skeleton-line skeleton-line--cell-md"></div>
            <div class="skeleton-line skeleton-line--cell-md"></div>
            <div class="skeleton-line skeleton-line--cell-md"></div>
            <div class="skeleton-line skeleton-line--cell-sm"></div>
          </div>
        </div>

        <div v-else-if="announcements.length === 0" class="empty-state">
          <div class="empty-icon">📢</div>
          <h3 class="empty-title">暂无公告</h3>
          <p class="empty-desc">发布公告，向用户传达重要信息</p>
          <el-button type="primary" class="empty-action" @click="openAnnouncementDialog()">发布公告</el-button>
        </div>

        <template v-else>
          <el-table :data="announcements" style="width: 100%">
            <el-table-column label="标题" min-width="150" prop="title" />
            <el-table-column label="关联游戏" width="120">
              <template #default="scope">
                {{ getGameName(scope.row.gameId) || '全部' }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="scope">
                <span v-if="scope.row.status === 1" class="status-badge status-badge--live">
                  <span class="status-dot status-dot--pulse"></span>活跃
                </span>
                <span v-else class="status-badge status-badge--finished">
                  <span class="status-dot"></span>停用
                </span>
              </template>
            </el-table-column>
            <el-table-column label="时间范围" width="220">
              <template #default="scope">
                <span class="ann-time-range">{{ formatDate(scope.row.startTime) }} ~ {{ formatDate(scope.row.endTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" width="160">
              <template #default="scope">
                {{ formatDate(scope.row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" align="center">
              <template #default="scope">
                <el-button link size="small" @click="openAnnouncementDialog(scope.row)">编辑</el-button>
                <el-button link size="small" class="delete-btn" @click="handleDeleteAnnouncement(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>

        <el-pagination
          v-if="announcementTotal > 0"
          background
          layout="prev, pager, next"
          :total="announcementTotal"
          :page-size="announcementPageSize"
          v-model:current-page="announcementPage"
          @current-change="fetchAnnouncements"
        />
      </el-tab-pane>

      <el-tab-pane label="游戏管理" name="game">
        <div class="tab-header">
          <span class="announcement-tab-title">游戏列表</span>
          <el-button type="primary" size="small" :icon="Plus" @click="openGameDialog()">添加游戏</el-button>
        </div>

        <!-- Loading Skeleton -->
        <div v-if="gameLoading" class="skeleton-table">
          <div class="skeleton-table-header">
            <div class="skeleton-line skeleton-line--cell-sm"></div>
            <div class="skeleton-line skeleton-line--cell-lg"></div>
            <div class="skeleton-line skeleton-line--cell-md"></div>
            <div class="skeleton-line skeleton-line--cell-sm"></div>
            <div class="skeleton-line skeleton-line--cell-sm"></div>
            <div class="skeleton-line skeleton-line--cell-sm"></div>
          </div>
          <div v-for="i in 6" :key="'game-skel-'+i" class="skeleton-table-row">
            <div class="skeleton-line skeleton-line--cell-sm"></div>
            <div class="skeleton-line skeleton-line--cell-lg"></div>
            <div class="skeleton-line skeleton-line--cell-md"></div>
            <div class="skeleton-line skeleton-line--cell-sm"></div>
            <div class="skeleton-line skeleton-line--cell-sm"></div>
            <div class="skeleton-line skeleton-line--cell-sm"></div>
          </div>
        </div>

        <!-- Empty State -->
        <div v-else-if="games.length === 0" class="empty-state">
          <div class="empty-icon">🎮</div>
          <h3 class="empty-title">还没有游戏</h3>
          <p class="empty-desc">添加游戏，让赛事按游戏分类管理</p>
          <el-button type="primary" class="empty-action" @click="openGameDialog()">添加游戏</el-button>
        </div>

        <!-- Game Table -->
        <template v-else>
          <el-table :data="games" style="width: 100%">
            <el-table-column label="ID" width="70" prop="id" />
            <el-table-column label="游戏名称" min-width="150" prop="name" />
            <el-table-column label="图标" width="120" align="center">
              <template #default="scope">
                <el-image
                  v-if="scope.row.icon"
                  :src="scope.row.icon"
                  style="width: 32px; height: 32px; border-radius: var(--radius-sm);"
                  fit="cover"
                />
                <span v-else class="game-icon-empty">-</span>
              </template>
            </el-table-column>
            <el-table-column label="排序" width="80" align="center" prop="sortOrder" />
            <el-table-column label="状态" width="100" align="center">
              <template #default="scope">
                <span v-if="scope.row.status === 1" class="status-badge status-badge--live">
                  <span class="status-dot status-dot--pulse"></span>启用
                </span>
                <span v-else class="status-badge status-badge--finished">
                  <span class="status-dot"></span>停用
                </span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" align="center">
              <template #default="scope">
                <el-button link size="small" @click="openGameDialog(scope.row)">编辑</el-button>
                <el-button link size="small" class="delete-btn" @click="handleDeleteGame(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </el-tab-pane>
    </el-tabs>

    <!-- Settle Dialog -->
    <el-dialog title="赛事结算" v-model="settleDialogVisible" width="400px" custom-class="settle-dialog">
      <div class="settle-content">
        <p class="settle-match-name">{{ settleMatch ? settleMatch.teamA + ' VS ' + settleMatch.teamB : '' }}</p>
        <p class="settle-label">选择比赛结果：</p>
        <el-radio-group v-model="settleResult">
          <el-radio :value="0">{{ settleMatch ? settleMatch.teamA : 'A队' }} 获胜</el-radio>
          <el-radio :value="1">{{ settleMatch ? settleMatch.teamB : 'B队' }} 获胜</el-radio>
          <el-radio :value="2">平局</el-radio>
        </el-radio-group>
      </div>
      <template #footer>
        <el-button @click="settleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="settleLoading" @click="handleSettle">确认结算</el-button>
      </template>
    </el-dialog>

    <!-- Announcement Dialog -->
    <el-dialog
      :title="announcementForm.id ? '编辑公告' : '发布公告'"
      v-model="announcementDialogVisible"
      width="520px"
    >
      <el-form :model="announcementForm" label-width="80px" class="announcement-form">
        <el-form-item label="标题">
          <el-input v-model="announcementForm.title" placeholder="公告标题" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="announcementForm.content"
            type="textarea"
            :rows="3"
            placeholder="公告内容"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="关联游戏">
          <el-select v-model="announcementForm.gameId" placeholder="全部游戏（可选）" clearable style="width: 100%">
            <el-option
              v-for="game in adminGames"
              :key="game.id"
              :label="game.name"
              :value="game.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="announcementForm.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="停用"
          />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="announcementForm.startTime"
            type="datetime"
            placeholder="选择开始时间"
            style="width: 100%"
            value-format="yyyy-MM-dd HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="announcementForm.endTime"
            type="datetime"
            placeholder="选择结束时间"
            style="width: 100%"
            value-format="yyyy-MM-dd HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="announcementForm.sortOrder" :min="0" :max="999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="announcementDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="announcementSubmitting" @click="handleSaveAnnouncement">
          {{ announcementForm.id ? '保存修改' : '发布' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- Game Dialog -->
    <el-dialog
      :title="gameForm.id ? '编辑游戏' : '添加游戏'"
      v-model="gameDialogVisible"
      width="480px"
    >
      <el-form :model="gameForm" label-width="80px" class="announcement-form">
        <el-form-item label="名称">
          <el-input v-model="gameForm.name" placeholder="例如: VALORANT" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="图标URL">
          <el-input v-model="gameForm.icon" placeholder="图标地址（可选）" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="gameForm.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="gameForm.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="停用"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="gameDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="gameSubmitting" @click="handleSaveGame">
          {{ gameForm.id ? '保存修改' : '添加' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { markRaw } from 'vue'
import request from '@/utils/request'
import { Plus, Refresh } from '@element-plus/icons-vue'

export default {
  name: 'AdminDashboard',
  components: { Plus, Refresh },
  data() {
    return {
      // 暴露图标组件供模板 :icon 绑定使用（markRaw 避免被响应式化）
      Plus: markRaw(Plus),
      Refresh: markRaw(Refresh),
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
      settleLoading: false,
      importJsonText: '',
      importParsedList: [],
      importParseError: '',
      importSubmitting: false,
      importResult: null,
      importErrorsExpanded: false,
      syncLoading: false,
      syncResult: null,
      lastSyncTime: null,
      announcements: [],
      announcementLoading: false,
      announcementPage: 1,
      announcementPageSize: 20,
      announcementTotal: 0,
      announcementDialogVisible: false,
      announcementSubmitting: false,
      announcementForm: {
        id: null,
        title: '',
        content: '',
        gameId: null,
        status: 1,
        startTime: '',
        endTime: '',
        sortOrder: 0
      },
      adminGames: [],
      games: [],
      gameLoading: false,
      gameDialogVisible: false,
      gameSubmitting: false,
      gameForm: {
        id: null,
        name: '',
        icon: '',
        sortOrder: 0,
        status: 1
      }
    }
  },
  created() {
    this.fetchMatches()
  },
  watch: {
    activeTab(tab) {
      if (tab === 'announcement' && this.announcements.length === 0) {
        this.fetchAnnouncements()
        this.fetchAdminGames()
      }
      if (tab === 'game' && this.games.length === 0) {
        this.fetchGames()
      }
    }
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
    },
    parseImportJson() {
      this.importParseError = ''
      this.importParsedList = []
      this.importResult = null
      this.importErrorsExpanded = false
      var text = this.importJsonText.trim()
      if (!text) {
        this.importParseError = '请输入 JSON 数据'
        return
      }
      var parsed
      try {
        parsed = JSON.parse(text)
      } catch (e) {
        this.importParseError = 'JSON 格式错误：' + e.message
        return
      }
      if (!Array.isArray(parsed)) {
        this.importParseError = 'JSON 必须是一个数组'
        return
      }
      if (parsed.length === 0) {
        this.importParseError = '数组不能为空'
        return
      }
      var required = ['title', 'teamA', 'teamB', 'matchTime']
      var errors = []
      for (var i = 0; i < parsed.length; i++) {
        var item = parsed[i]
        for (var j = 0; j < required.length; j++) {
          if (!item[required[j]]) {
            errors.push('第 ' + (i + 1) + ' 条缺少必填字段：' + required[j])
          }
        }
      }
      if (errors.length > 0) {
        this.importParseError = errors.join('；')
        return
      }
      this.importParsedList = parsed
    },
    clearImport() {
      this.importJsonText = ''
      this.importParsedList = []
      this.importParseError = ''
      this.importResult = null
      this.importErrorsExpanded = false
    },
    submitBatchImport() {
      if (this.importParsedList.length === 0) return
      this.importSubmitting = true
      this.importResult = null
      this.importErrorsExpanded = false
      request.post('/match/batch-import', this.importParsedList).then(res => {
        var data = res.data || res
        this.importResult = data
        if (data.success > 0) {
          this.fetchMatches()
        }
      }).catch(err => {
        var msg = (err && err.response && err.response.data && err.response.data.message) || '导入请求失败'
        this.importResult = { success: 0, fail: this.importParsedList.length, total: this.importParsedList.length, errors: [{ index: 0, title: '-', error: msg }] }
      }).finally(() => {
        this.importSubmitting = false
      })
    },
    statusLabel(status) {
      if (status === 0) return '未开始'
      if (status === 1) return '进行中'
      if (status === 2) return '已结束'
      return '未开始'
    },
    statusBadgeClass(status) {
      if (status === 0) return 'status-badge--upcoming'
      if (status === 1) return 'status-badge--live'
      if (status === 2) return 'status-badge--finished'
      return 'status-badge--upcoming'
    },
    // 将 perGameStats 的 raw key（如 "game-1"）映射为人类可读的游戏名
    getGameNameByKey(key) {
      var map = {
        'game-1': '无畏契约',
        'game-2': '英雄联盟',
        'game-3': 'Dota 2',
        'game-4': 'CS2'
      }
      return map[key] || key
    },
    triggerSync() {
      this.syncLoading = true
      this.syncResult = null
      // 同步涉及多个外部 API（Riot/OpenDota/GRID），整体耗时较长，
      // 单独放宽到 120 秒，避免默认 15 秒超时导致前端一直 loading
      request.post('/match/sync', {}, { timeout: 120000 }).then(res => {
        var data = res.data || res
        this.syncResult = data
        if (data.syncTime) {
          this.lastSyncTime = data.syncTime
        }
      }).catch(err => {
        var msg = (err && err.response && err.response.data && err.response.data.message) || (err && err.message) || '未知错误'
        this.$message.error('同步失败: ' + msg)
      }).finally(() => {
        this.syncLoading = false
      })
    },
    formatSyncTime(timeStr) {
      if (!timeStr) return '-'
      var d = new Date(timeStr)
      var y = d.getFullYear()
      var m = (d.getMonth() + 1).toString().padStart(2, '0')
      var day = d.getDate().toString().padStart(2, '0')
      var h = d.getHours().toString().padStart(2, '0')
      var min = d.getMinutes().toString().padStart(2, '0')
      var sec = d.getSeconds().toString().padStart(2, '0')
      return y + '-' + m + '-' + day + ' ' + h + ':' + min + ':' + sec
    },
    fetchAnnouncements() {
      this.announcementLoading = true
      request.get('/announcement/list', {
        params: { page: this.announcementPage, size: this.announcementPageSize }
      }).then(res => {
        const data = res.data || res
        if (Array.isArray(data)) {
          this.announcements = data
          this.announcementTotal = data.length
        } else if (data.records) {
          this.announcements = data.records
          this.announcementTotal = data.total || data.records.length
        } else {
          this.announcements = data.list || data.content || []
          this.announcementTotal = data.total || data.totalElements || 0
        }
      }).catch(() => {}).finally(() => {
        this.announcementLoading = false
      })
    },
    fetchAdminGames() {
      request.get('/game/list').then(res => {
        const data = res.data || res
        if (Array.isArray(data)) {
          this.adminGames = data
        } else if (data.records) {
          this.adminGames = data.records
        } else {
          this.adminGames = data.list || data.content || []
        }
      }).catch(() => {
        this.adminGames = []
      })
    },
    getGameName(gameId) {
      if (!gameId) return ''
      var game = this.adminGames.find(function (g) { return g.id === gameId })
      return game ? game.name : ''
    },
    openAnnouncementDialog(row) {
      if (row) {
        this.announcementForm = {
          id: row.id,
          title: row.title || '',
          content: row.content || '',
          gameId: row.gameId || null,
          status: row.status != null ? row.status : 1,
          startTime: row.startTime || '',
          endTime: row.endTime || '',
          sortOrder: row.sortOrder || 0
        }
      } else {
        this.announcementForm = {
          id: null,
          title: '',
          content: '',
          gameId: null,
          status: 1,
          startTime: '',
          endTime: '',
          sortOrder: 0
        }
      }
      this.announcementDialogVisible = true
    },
    handleSaveAnnouncement() {
      if (!this.announcementForm.title || !this.announcementForm.title.trim()) {
        this.$message.warning('请输入公告标题')
        return
      }
      if (!this.announcementForm.content || !this.announcementForm.content.trim()) {
        this.$message.warning('请输入公告内容')
        return
      }
      this.announcementSubmitting = true
      var payload = {
        title: this.announcementForm.title,
        content: this.announcementForm.content,
        gameId: this.announcementForm.gameId || null,
        status: this.announcementForm.status,
        startTime: this.announcementForm.startTime || null,
        endTime: this.announcementForm.endTime || null,
        sortOrder: this.announcementForm.sortOrder || 0
      }
      var promise
      if (this.announcementForm.id) {
        promise = request.put('/announcement/' + this.announcementForm.id, payload)
      } else {
        promise = request.post('/announcement', payload)
      }
      promise.then(() => {
        this.$message.success(this.announcementForm.id ? '修改成功' : '发布成功')
        this.announcementDialogVisible = false
        this.fetchAnnouncements()
      }).catch(() => {}).finally(() => {
        this.announcementSubmitting = false
      })
    },
    handleDeleteAnnouncement(row) {
      this.$confirm('确定删除公告「' + row.title + '」？', '确认删除', {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        request.delete('/announcement/' + row.id).then(() => {
          this.$message.success('删除成功')
          this.fetchAnnouncements()
        }).catch(() => {})
      }).catch(() => {})
    },
    fetchGames() {
      this.gameLoading = true
      request.get('/game/list').then(res => {
        const data = res.data || res
        if (Array.isArray(data)) {
          this.games = data
        } else if (data.records) {
          this.games = data.records
        } else {
          this.games = data.list || data.content || []
        }
      }).catch(() => {
        this.games = []
      }).finally(() => {
        this.gameLoading = false
      })
    },
    openGameDialog(row) {
      if (row) {
        this.gameForm = {
          id: row.id,
          name: row.name || '',
          icon: row.icon || '',
          sortOrder: row.sortOrder != null ? row.sortOrder : 0,
          status: row.status != null ? row.status : 1
        }
      } else {
        this.gameForm = {
          id: null,
          name: '',
          icon: '',
          sortOrder: 0,
          status: 1
        }
      }
      this.gameDialogVisible = true
    },
    handleSaveGame() {
      if (!this.gameForm.name || !this.gameForm.name.trim()) {
        this.$message.warning('请输入游戏名称')
        return
      }
      this.gameSubmitting = true
      var payload = {
        name: this.gameForm.name,
        icon: this.gameForm.icon || null,
        sortOrder: this.gameForm.sortOrder != null ? this.gameForm.sortOrder : 0,
        status: this.gameForm.status
      }
      var promise
      if (this.gameForm.id) {
        promise = request.put('/game/' + this.gameForm.id, payload)
      } else {
        promise = request.post('/game', payload)
      }
      promise.then(() => {
        this.$message.success('保存成功')
        this.gameDialogVisible = false
        this.fetchGames()
        this.fetchAdminGames()
      }).catch(() => {}).finally(() => {
        this.gameSubmitting = false
      })
    },
    handleDeleteGame(row) {
      var tip = row.status === 1 ? '游戏当前为启用状态，' : ''
      this.$confirm(tip + '确定删除游戏「' + row.name + '」？', '确认删除', {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        request.delete('/game/' + row.id).then(() => {
          this.$message.success('删除成功')
          this.fetchGames()
          this.fetchAdminGames()
        }).catch(() => {})
      }).catch(() => {})
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

/* Import Panel */
.import-panel {
  padding: 4px 0;
}

.import-instructions {
  margin-bottom: 16px;
  padding: 16px;
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
}

.import-section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px 0;
}

.import-hint {
  font-size: 13px;
  color: var(--text-tertiary);
  margin: 0;
  line-height: 1.6;
}

.import-textarea {
  margin-bottom: 12px;
}

.import-textarea :deep(.el-textarea__inner) {
  background: var(--bg-surface-1);
  border-color: var(--border-subtle);
  color: var(--text-primary);
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  border-radius: var(--radius-md);
  resize: vertical;
}

.import-textarea :deep(.el-textarea__inner:focus) {
  border-color: var(--accent-primary);
}

.import-textarea :deep(.el-textarea__inner::placeholder) {
  color: var(--text-tertiary);
  opacity: 0.6;
}

.import-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.import-actions .el-button--primary {
  background: var(--accent-primary) !important;
  border-color: var(--accent-primary) !important;
  color: #0a0a0f !important;
  font-weight: 600;
  border-radius: var(--radius-sm) !important;
}

.import-parse-error {
  margin-bottom: 16px;
}

.import-preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding: 10px 16px;
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
}

.import-preview-count {
  font-size: 14px;
  font-weight: 600;
  color: var(--accent-primary);
}

.import-submit-row {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.import-submit-row .el-button--primary {
  background: var(--accent-primary) !important;
  border-color: var(--accent-primary) !important;
  color: #0a0a0f !important;
  font-weight: 600;
  border-radius: var(--radius-md) !important;
  padding: 10px 24px;
}

.import-result {
  margin-top: 20px;
}

.import-errors {
  margin-top: 12px;
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.import-errors-toggle {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  cursor: pointer;
  color: var(--text-secondary);
  font-size: 13px;
  transition: color 0.2s;
  user-select: none;
}

.import-errors-toggle:hover {
  color: var(--text-primary);
}

.import-errors-list {
  border-top: 1px solid var(--border-subtle);
}

.import-error-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  font-size: 13px;
  border-bottom: 1px solid var(--border-subtle);
}

.import-error-item:last-child {
  border-bottom: none;
}

.import-error-index {
  color: var(--text-tertiary);
  font-weight: 600;
  min-width: 32px;
}

.import-error-title {
  color: var(--text-primary);
  min-width: 100px;
}

.import-error-msg {
  color: #e06c75;
  flex: 1;
}

/* Sync Panel */
.sync-panel {
  padding: 4px 0;
}

.sync-action-area {
  margin-bottom: 24px;
  padding: 20px;
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
}

.sync-action-area .el-button--primary {
  background: var(--accent-primary) !important;
  border-color: var(--accent-primary) !important;
  color: #0a0a0f !important;
  font-weight: 600;
  border-radius: var(--radius-md) !important;
  padding: 10px 24px;
}

.sync-action-area .el-button--primary.is-disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.sync-last-time {
  margin: 12px 0 0 0;
  font-size: 13px;
  color: var(--text-tertiary);
}

/* Sync Skeleton */
.sync-skeleton {
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  padding: 20px;
}

.sync-skeleton-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.sync-skeleton-card {
  padding: 16px;
  background: var(--bg-surface-2);
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.skeleton-line--stat-label {
  width: 60%;
  height: 12px;
  background: linear-gradient(90deg, var(--bg-surface-3) 25%, var(--bg-surface-2) 50%, var(--bg-surface-3) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
  border-radius: var(--radius-sm);
}

.skeleton-line--stat-value {
  width: 40%;
  height: 24px;
  background: linear-gradient(90deg, var(--bg-surface-3) 25%, var(--bg-surface-2) 50%, var(--bg-surface-3) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
  border-radius: var(--radius-sm);
}

/* Sync Results */
.sync-results {
  margin-top: 0;
}

.sync-stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.sync-stat-card {
  padding: 16px 20px;
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.sync-stat-value {
  font-size: 28px;
  font-weight: 700;
  font-family: var(--font-display);
  line-height: 1.2;
}

.sync-stat-value--gray { color: var(--text-secondary); }
.sync-stat-value--green { color: #67c23a; }
.sync-stat-value--blue { color: #409eff; }
.sync-stat-value--orange { color: #e6a23c; }
.sync-stat-value--purple { color: #9254de; }

.sync-stat-label {
  font-size: 13px;
  color: var(--text-tertiary);
}

/* Sync Errors */
.sync-errors {
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.sync-per-game {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 10px 16px;
  margin-top: 8px;
  background: var(--bg-surface-3);
  border-radius: var(--radius-md);
}

.sync-per-game-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
}

.sync-per-game-label {
  color: var(--text-tertiary);
  font-family: var(--font-mono);
}

.sync-per-game-val {
  color: var(--text-secondary);
  font-family: var(--font-mono);
}

.sync-errors-title {
  padding: 10px 16px;
  font-size: 13px;
  font-weight: 600;
  color: #e06c75;
  border-bottom: 1px solid var(--border-subtle);
}

.sync-errors-list {
  padding: 0;
}

.sync-error-item {
  padding: 8px 16px;
  font-size: 13px;
  color: #e06c75;
  border-bottom: 1px solid var(--border-subtle);
  line-height: 1.5;
}

.sync-error-item:last-child {
  border-bottom: none;
}

/* Announcement Tab */
.announcement-tab-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.delete-btn {
  color: var(--accent-danger) !important;
}

.ann-time-range {
  font-size: 12px;
  color: var(--text-tertiary);
  font-family: var(--font-mono);
}

.announcement-form .el-form-item {
  margin-bottom: 18px;
}

.announcement-form .el-textarea__inner {
  background: var(--bg-surface-1) !important;
  border-color: var(--border-default) !important;
  color: var(--text-primary) !important;
  border-radius: var(--radius-md) !important;
}

.announcement-form .el-textarea__inner:focus {
  border-color: var(--accent-primary) !important;
}

.announcement-form .el-select .el-input__inner {
  cursor: pointer;
}

.announcement-form .el-input-number {
  width: 160px;
}

.announcement-form .el-switch__label {
  color: var(--text-tertiary);
}

.announcement-form .el-switch__label.is-active {
  color: var(--accent-primary);
}

/* Game Tab */
.game-icon-empty {
  color: var(--text-tertiary);
  font-size: 14px;
}

/* ===== 移动端基础适配 ===== */
@media (max-width: 768px) {
  .page-title {
    font-size: 26px;
  }

  /* tab 头部：筛选与操作按钮改为纵向堆叠，避免横向溢出 */
  .tab-header {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }
  .tab-header .el-button {
    width: 100%;
  }

  /* 表格容器保证可横向滚动 */
  :deep(.el-table) {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }
  /* 操作列按钮 touch-friendly */
  :deep(.el-table .el-button) {
    min-height: 32px;
    padding: 4px 8px;
  }

  /* 同步统计/骨架网格：3 列 → 2 列 */
  .sync-stats-grid,
  .sync-skeleton-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .sync-stat-card {
    padding: 12px 14px;
  }
  .sync-stat-value {
    font-size: 24px;
  }
  .sync-action-area {
    padding: 14px;
  }
  .sync-action-area .el-button--primary {
    width: 100%;
    min-height: 44px;
  }

  /* 导入错误项：窄屏堆叠 */
  .import-error-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
  .import-error-title {
    min-width: 0;
  }
  .import-submit-row {
    justify-content: stretch;
  }
  .import-submit-row .el-button--primary {
    width: 100%;
    min-height: 44px;
  }

  /* 公告表单 label 宽度收紧 */
  .announcement-form :deep(.el-form-item__label) {
    width: 64px !important;
  }
  .announcement-form :deep(.el-input-number) {
    width: 140px !important;
  }
}

@media (max-width: 480px) {
  .page-title {
    font-size: 22px;
  }
  /* 同步统计在小屏单列堆叠 */
  .sync-stats-grid,
  .sync-skeleton-grid {
    grid-template-columns: 1fr;
  }
}
</style>