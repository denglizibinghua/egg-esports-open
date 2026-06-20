<template>
  <el-dialog
    title="裁剪头像"
    :visible.sync="dialogVisible"
    width="520px"
    :close-on-click-modal="false"
    custom-class="cropper-dialog"
    @closed="onClosed"
  >
    <!-- Step 1: 选择图片 -->
    <div v-if="!imgSrc" class="cropper-select">
      <div class="cropper-select-icon">
        <svg viewBox="0 0 80 80" width="80" height="80">
          <rect x="10" y="15" width="60" height="50" rx="6" fill="none" stroke="var(--border-default)" stroke-width="2"/>
          <circle cx="40" cy="35" r="12" fill="none" stroke="var(--accent-primary)" stroke-width="2" opacity="0.4"/>
          <path d="M10 55 L30 40 L42 48 L56 32 L70 38 L70 65 L10 65 Z" fill="var(--bg-surface-3)"/>
        </svg>
      </div>
      <p class="cropper-select-hint">支持 JPG / PNG / WebP，建议 200×200 以上</p>
      <el-button type="primary" @click="pickFile">选择图片</el-button>
      <input ref="fileInput" type="file" accept="image/*" style="display:none" @change="onFileChange" />
    </div>

    <!-- Step 2: 裁剪区域 -->
    <div v-else class="cropper-body">
      <div class="cropper-box">
        <vue-cropper
          ref="cropper"
          :img="imgSrc"
          :autoCrop="true"
          :fixed="true"
          :fixedNumber="[1, 1]"
          :centerBox="true"
          :canMove="true"
          :canMoveBox="false"
          :canScale="true"
          :autoCropWidth="240"
          :autoCropHeight="240"
          :outputType="'png'"
          :outputSize="1"
          :info="true"
          @imgLoad="onImgLoad"
        />
        <!-- 圆形遮罩 -->
        <div class="circle-mask"></div>
      </div>

      <!-- 控制栏 -->
      <div class="cropper-controls">
        <div class="cropper-controls-left">
          <el-button-group size="small">
            <el-button icon="el-icon-zoom-out" @click="zoom(-0.15)"></el-button>
            <el-button icon="el-icon-zoom-in" @click="zoom(0.15)"></el-button>
          </el-button-group>
          <el-button-group size="small" style="margin-left:6px">
            <el-button icon="el-icon-refresh-left" @click="rotateL"></el-button>
            <el-button icon="el-icon-refresh-right" @click="rotateR"></el-button>
          </el-button-group>
          <el-button-group size="small" style="margin-left:6px">
            <el-button @click="reset">重置</el-button>
          </el-button-group>
        </div>
      </div>
    </div>

    <span slot="footer">
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="uploading" :disabled="!imgSrc" @click="confirm">
        保存头像
      </el-button>
    </span>
  </el-dialog>
</template>

<script>
import { VueCropper } from 'vue-cropper'
import request from '@/utils/request'

export default {
  name: 'AvatarCropper',
  components: { VueCropper },
  props: {
    value: { type: Boolean, default: false }
  },
  data() {
    return {
      imgSrc: '',
      uploading: false
    }
  },
  computed: {
    dialogVisible: {
      get() { return this.value },
      set(v) { this.$emit('input', v) }
    }
  },
  methods: {
    pickFile() {
      this.$refs.fileInput.click()
    },
    onFileChange(e) {
      var file = e.target.files[0]
      if (!file) return
      if (!/^image\/(png|jpe?g|webp)$/i.test(file.type)) {
        this.$message.error('仅支持 PNG / JPG / WebP 格式')
        return
      }
      if (file.size > 5 * 1024 * 1024) {
        this.$message.error('图片不能超过 5MB')
        return
      }
      var reader = new FileReader()
      var self = this
      reader.onload = function (ev) { self.imgSrc = ev.target.result }
      reader.readAsDataURL(file)
    },
    onImgLoad(status) {
      if (status !== 'success') {
        this.$message.error('图片加载失败，请重试')
      }
    },
    zoom(delta) {
      this.$refs.cropper.changeScale(delta)
    },
    rotateR() {
      this.$refs.cropper.rotateRight()
    },
    rotateL() {
      this.$refs.cropper.rotateLeft()
    },
    reset() {
      this.$refs.cropper.refresh()
    },
    confirm() {
      var self = this
      this.$refs.cropper.getCropBlob(function (blob) {
        if (!blob) return
        self.uploading = true
        var formData = new FormData()
        formData.append('file', blob, 'avatar.png')
        request.post('/user/avatar', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        }).then(function (res) {
          var data = res.data || res
          var avatarUrl = data.avatar
          if (avatarUrl) {
            self.$emit('cropped', avatarUrl)
            self.$message.success('头像更新成功')
            self.dialogVisible = false
          }
        }).catch(function () {
          self.$message.error('头像上传失败')
        }).finally(function () {
          self.uploading = false
        })
      })
    },
    onClosed() {
      this.imgSrc = ''
      if (this.$refs.fileInput) {
        this.$refs.fileInput.value = ''
      }
    }
  }
}
</script>

<style scoped>
/* ── 选择区域 ── */
.cropper-select {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32px 0 16px;
  gap: 16px;
}
.cropper-select-icon {
  opacity: 0.5;
}
.cropper-select-hint {
  font-size: 13px;
  color: var(--text-tertiary);
}

/* ── 裁剪区域 ── */
.cropper-body {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.cropper-box {
  position: relative;
  width: 100%;
  height: 360px;
  background: var(--bg-page);
  border-radius: var(--radius-md);
  overflow: hidden;
  touch-action: none;
}
/* 圆形黑色遮罩：在 1:1 裁剪框外显示暗色覆盖 */
.circle-mask {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 240px;
  height: 240px;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.55);
  pointer-events: none;
}

/* ── 控制栏 ── */
.cropper-controls {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  margin-top: 14px;
  gap: 16px;
}
.cropper-controls-left {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

/* ── vue-cropper 内部暗色覆盖 ── */
.cropper-box >>> .vue-cropper .cropper-modal {
  background: rgba(0, 0, 0, 0.65) !important;
}
.cropper-box >>> .vue-cropper .cropper-view-box {
  outline: 1px solid rgba(255, 255, 255, 0.25);
  outline-offset: -1px;
}
.cropper-box >>> .vue-cropper .cropper-face {
  background: transparent !important;
}
.cropper-box >>> .vue-cropper .cropper-line {
  background: transparent !important;
}
.cropper-box >>> .vue-cropper .cropper-point {
  background: var(--accent-primary);
  border: 2px solid var(--bg-surface-1);
  width: 10px;
  height: 10px;
}

/* ── 按钮暗色主题 ── */
.cropper-controls >>> .el-button,
.cropper-controls >>> .el-button-group .el-button {
  background: var(--bg-surface-2);
  border-color: var(--border-default);
  color: var(--text-secondary);
  transition: all var(--duration-fast) ease;
}
.cropper-controls >>> .el-button:hover {
  background: var(--bg-surface-3);
  border-color: var(--border-strong);
  color: var(--text-primary);
}
.cropper-controls >>> .el-button:active {
  background: var(--bg-surface-1);
}

/* ── 底部按钮 ── */
.cropper-dialog .el-dialog__footer .el-button--default {
  background: var(--bg-surface-2);
  border-color: var(--border-default);
  color: var(--text-secondary);
}
.cropper-dialog .el-dialog__footer .el-button--default:hover {
  background: var(--bg-surface-3);
  border-color: var(--border-strong);
  color: var(--text-primary);
}
</style>

<!-- 全局样式：让 el-dialog 内部适应暗色 -->
<style>
.cropper-dialog {
  background: var(--bg-surface-2) !important;
  border: 1px solid var(--border-default) !important;
  border-radius: var(--radius-xl) !important;
}
</style>
