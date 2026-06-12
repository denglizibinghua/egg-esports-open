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
      rules: {
        title: [{ required: true, message: '请输入赛事标题', trigger: 'blur' }],
        teamA: [{ required: true, message: '请输入A队名称', trigger: 'blur' }],
        teamB: [{ required: true, message: '请输入B队名称', trigger: 'blur' }],
        matchTime: [{ required: true, message: '请选择比赛时间', trigger: 'change' }]
      }
    }
  },
  created() {
    this.fetchMatch()
  },
  methods: {
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
      }).catch(() => {
        this.$message.error('赛事不存在')
        this.$router.back()
      }).finally(() => {
        this.loading = false
      })
    },
    handleSubmit() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.submitLoading = true
        request.put('/match/update', this.form).then(() => {
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
  color: #888888 !important;
}

.back-btn:hover {
  color: #6aaa96 !important;
}

.page-title {
  color: #d4d4d4;
  font-size: 24px;
  font-weight: 700;
}

.form-card {
  background-color: #1a1a1a;
  border: 1px solid #2e2e2e;
  border-radius: 12px;
  padding: 32px;
  max-width: 600px;
}

.submit-btn {
  background-color: #6aaa96;
  border-color: #6aaa96;
}

.submit-btn:hover {
  background-color: #7bc4a8;
  border-color: #7bc4a8;
}
</style>