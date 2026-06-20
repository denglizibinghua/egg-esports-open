<template>
  <div class="match-edit" v-loading="loading">
    <div class="page-header">
      <el-button type="text" icon="el-icon-arrow-left" class="back-btn" @click="$router.back()">返回</el-button>
      <h2 class="page-title">编辑赛事</h2>
    </div>

    <div v-if="form" class="form-card">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px" class="edit-form">
        <el-form-item label="赛事标题" prop="title">
          <el-input v-model="form.title" placeholder="例如: T1 vs Gen.G" />
        </el-form-item>

        <el-form-item label="A队名称" prop="teamA">
          <el-input v-model="form.teamA" placeholder="A队名称" />
        </el-form-item>

        <el-form-item label="B队名称" prop="teamB">
          <el-input v-model="form.teamB" placeholder="B队名称" />
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

        <el-form-item label="比赛时间" prop="matchTime">
          <el-date-picker
            v-model="form.matchTime"
            type="datetime"
            placeholder="选择比赛时间"
            value-format="yyyy-MM-ddTHH:mm:ss"
          />
        </el-form-item>

        <el-form-item label="赛事状态">
          <el-select v-model="form.status" placeholder="选择状态">
            <el-option :value="0" label="未开始" />
            <el-option :value="1" label="进行中" />
            <el-option :value="2" label="已结束" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" class="submit-btn" :loading="submitLoading" @click="handleSubmit">保存修改</el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'MatchEdit',
  data() {
    return {
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
        if (Array.isArray(data)) {
          this.games = data
        } else if (data.records) {
          this.games = data.records
        } else {
          this.games = data.list || data.content || []
        }
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
.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.back-btn {
  color: var(--text-tertiary) !important;
  transition: color var(--duration-fast) ease;
}

.back-btn:hover {
  color: var(--accent-primary) !important;
}

.page-title {
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 32px;
  letter-spacing: 1px;
}

.form-card {
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-xl);
  padding: 32px;
  max-width: 600px;
  box-shadow: var(--shadow-md);
}

.submit-btn {
  background: var(--accent-primary) !important;
  border-color: var(--accent-primary) !important;
  color: #0a0a0f !important;
  font-weight: 600;
  border-radius: var(--radius-md) !important;
  transition: transform var(--duration-fast) var(--ease-out),
              box-shadow var(--duration-fast) var(--ease-out);
}

.submit-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 0 20px rgba(0,229,160,0.25);
}
</style>