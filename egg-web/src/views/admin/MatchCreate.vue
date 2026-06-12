<template>
  <div class="match-create">
    <div class="page-header">
      <el-button type="text" icon="el-icon-arrow-left" class="back-btn" @click="$router.back()">返回</el-button>
      <h2 class="page-title">创建赛事</h2>
    </div>

    <div class="form-card">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px" class="create-form">
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
            value-format="yyyy-MM-dd HH:mm:ss"
            :picker-options="pickerOptions"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" class="submit-btn" :loading="submitLoading" @click="handleSubmit">创建赛事</el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'MatchCreate',
  data() {
    return {
      form: {
        title: '',
        teamA: '',
        teamB: '',
        tournament: '',
        matchTime: '',
        gameId: 1
      },
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
  methods: {
    handleSubmit() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.submitLoading = true
        request.post('/match/create', this.form).then(() => {
          this.$message.success('赛事创建成功')
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
  color: #a0a0b0 !important;
}

.back-btn:hover {
  color: #FF4655 !important;
}

.page-title {
  color: #e0e0e0;
  font-size: 24px;
  font-weight: 700;
}

.form-card {
  background-color: #16213e;
  border: 1px solid #2a2a4a;
  border-radius: 12px;
  padding: 32px;
  max-width: 600px;
}

.submit-btn {
  background-color: #FF4655;
  border-color: #FF4655;
}

.submit-btn:hover {
  background-color: #e63e4d;
  border-color: #e63e4d;
}
</style>