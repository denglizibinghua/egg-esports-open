<template>
  <el-dialog
    title="裁剪头像"
    v-model="dialogVisible"
    width="520px"
    :close-on-click-modal="false"
    class="cropper-dialog"
    @closed="onClosed"
  >
    <!-- Step 1: 选择图片 -->
    <div v-if="!imgSrc" class="cropper-select">
      <div class="select-frame">
        <span class="frame-corner frame-corner--tl"></span>
        <span class="frame-corner frame-corner--tr"></span>
        <span class="frame-corner frame-corner--bl"></span>
        <span class="frame-corner frame-corner--br"></span>
        <svg class="select-illustration" viewBox="0 0 80 80" width="80" height="80" aria-hidden="true">
          <rect class="il-frame" x="10" y="15" width="60" height="50" rx="4" />
          <circle class="il-lens" cx="40" cy="35" r="11" />
          <path class="il-mountain" d="M10 55 L30 40 L42 48 L56 32 L70 38 L70 61 L10 61 Z" />
        </svg>
      </div>
      <p class="cropper-select-title">选择一张图片</p>
      <p class="cropper-select-hint">支持 JPG / PNG / WebP · 建议 200×200 以上 · 最大 5MB</p>
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
        <!-- 裁剪框红色直角 -->
        <div class="crop-frame" aria-hidden="true">
          <span class="crop-corner crop-corner--tl"></span>
          <span class="crop-corner crop-corner--tr"></span>
          <span class="crop-corner crop-corner--bl"></span>
          <span class="crop-corner crop-corner--br"></span>
          <span class="crop-ratio">1 : 1</span>
        </div>
      </div>

      <!-- 控制栏 -->
      <div class="cropper-controls">
        <div class="control-group">
          <span class="control-label">ZOOM</span>
          <el-button-group size="small">
            <el-button :icon="ZoomOut" title="缩小" @click="zoom(-0.15)"></el-button>
            <el-button :icon="ZoomIn" title="放大" @click="zoom(0.15)"></el-button>
          </el-button-group>
        </div>
        <div class="control-group">
          <span class="control-label">ROTATE</span>
          <el-button-group size="small">
            <el-button :icon="RefreshLeft" title="逆时针旋转" @click="rotateL"></el-button>
            <el-button :icon="RefreshRight" title="顺时针旋转" @click="rotateR"></el-button>
          </el-button-group>
        </div>
        <div class="control-group">
          <el-button size="small" class="reset-btn" @click="reset">重置</el-button>
        </div>
        <span class="crop-size">OUTPUT 240 × 240 · PNG</span>
      </div>
    </div>

    <template #footer>
      <span>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" :disabled="!imgSrc" @click="confirm">
          保存头像
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script>
import { VueCropper } from 'vue-cropper'
import 'vue-cropper/dist/index.css'
import { markRaw } from 'vue'
import { ZoomIn, ZoomOut, RefreshLeft, RefreshRight } from '@element-plus/icons-vue'
import request from '@/utils/request'

export default {
  name: 'AvatarCropper',
  components: { VueCropper },
  props: {
    modelValue: { type: Boolean, default: false }
  },
  data() {
    return {
      imgSrc: '',
      uploading: false,
      // 用 markRaw 包装避免图标组件被响应化触发 Vue 警告
      ZoomIn: markRaw(ZoomIn),
      ZoomOut: markRaw(ZoomOut),
      RefreshLeft: markRaw(RefreshLeft),
      RefreshRight: markRaw(RefreshRight)
    }
  },
  computed: {
    dialogVisible: {
      get() { return this.modelValue },
      set(v) { this.$emit('update:modelValue', v) }
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
/* ---------- Step 1: select ---------- */
.cropper-select {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 12px 0 6px;
}

.select-frame {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 124px;
  height: 124px;
  background:
    linear-gradient(var(--border-subtle) 1px, transparent 1px) 0 0 / 100% 20px,
    linear-gradient(90deg, var(--border-subtle) 1px, transparent 1px) 0 0 / 20px 100%,
    var(--bg-inset);
  border: 1px dashed var(--border-default);
  border-radius: var(--radius-lg);
}

.frame-corner {
  position: absolute;
  width: 12px;
  height: 12px;
  border-style: solid;
  border-color: var(--brand);
  pointer-events: none;
}

.frame-corner--tl {
  top: -1px;
  left: -1px;
  border-width: 2px 0 0 2px;
}

.frame-corner--tr {
  top: -1px;
  right: -1px;
  border-width: 2px 2px 0 0;
}

.frame-corner--bl {
  bottom: -1px;
  left: -1px;
  border-width: 0 0 2px 2px;
}

.frame-corner--br {
  bottom: -1px;
  right: -1px;
  border-width: 0 2px 2px 0;
}

.select-illustration {
  opacity: 0.9;
}

.il-frame {
  fill: none;
  stroke: var(--border-default);
  stroke-width: 2;
}

.il-lens {
  fill: none;
  stroke: var(--brand);
  stroke-width: 2;
  opacity: 0.55;
}

.il-mountain {
  fill: var(--bg-surface-3);
  stroke: var(--border-default);
  stroke-width: 1;
}

.cropper-select-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
}

.cropper-select-hint {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.04em;
  color: var(--text-tertiary);
  text-align: center;
  max-width: 340px;
  line-height: 1.6;
}

/* ---------- Step 2: crop ---------- */
.cropper-body {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.cropper-box {
  position: relative;
  width: 100%;
  height: 340px;
  background:
    linear-gradient(var(--border-subtle) 1px, transparent 1px) 0 0 / 100% 28px,
    linear-gradient(90deg, var(--border-subtle) 1px, transparent 1px) 0 0 / 28px 100%,
    var(--bg-inset);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  overflow: hidden;
  touch-action: none;
}

/* 圆形暗色遮罩：在 1:1 裁剪框外压暗 */
.circle-mask {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 240px;
  height: 240px;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  box-shadow: 0 0 0 9999px color-mix(in srgb, var(--bg-page) 58%, transparent);
  pointer-events: none;
}

/* 裁剪框红色直角 */
.crop-frame {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 240px;
  height: 240px;
  transform: translate(-50%, -50%);
  pointer-events: none;
}

.crop-corner {
  position: absolute;
  width: 18px;
  height: 18px;
  border-style: solid;
  border-color: var(--brand);
}

.crop-corner--tl {
  top: -1px;
  left: -1px;
  border-width: 2px 0 0 2px;
}

.crop-corner--tr {
  top: -1px;
  right: -1px;
  border-width: 2px 2px 0 0;
}

.crop-corner--bl {
  bottom: -1px;
  left: -1px;
  border-width: 0 0 2px 2px;
}

.crop-corner--br {
  bottom: -1px;
  right: -1px;
  border-width: 0 2px 2px 0;
}

.crop-ratio {
  position: absolute;
  right: 6px;
  bottom: 6px;
  padding: 2px 6px;
  font-family: var(--font-mono);
  font-size: 8px;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: var(--text-secondary);
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
}

/* ---------- Controls ---------- */
.cropper-controls {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px 18px;
  width: 100%;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid var(--border-subtle);
}

.control-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.control-label {
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.14em;
  color: var(--text-faint);
}

.cropper-controls :deep(.el-button) {
  width: 32px;
  height: 32px !important;
  min-height: 32px !important;
  padding: 0 !important;
  background: var(--bg-surface-2);
  border-color: var(--border-default);
  color: var(--text-secondary);
}

.cropper-controls :deep(.el-button:hover) {
  border-color: var(--brand-line);
  color: var(--brand);
  background: var(--bg-surface-3);
}

.cropper-controls :deep(.reset-btn) {
  width: auto;
  padding: 0 14px !important;
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.06em;
}

.crop-size {
  margin-left: auto;
  font-family: var(--font-mono);
  font-size: 8px;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: var(--text-faint);
  white-space: nowrap;
}

/* ---------- vue-cropper internals ---------- */
.cropper-box :deep(.vue-cropper .cropper-modal) {
  background: var(--bg-page);
  opacity: 0.4;
}

.cropper-box :deep(.vue-cropper .cropper-view-box) {
  outline: 1px solid var(--brand-line);
  outline-offset: -1px;
}

.cropper-box :deep(.vue-cropper .cropper-face) {
  background: transparent;
}

.cropper-box :deep(.vue-cropper .cropper-line) {
  background: transparent;
}

.cropper-box :deep(.vue-cropper .cropper-point) {
  display: none;
}

/* ---------- Responsive ---------- */
@media (max-width: 480px) {
  .cropper-box {
    height: 300px;
  }

  .cropper-controls {
    gap: 10px 12px;
  }

  .crop-size {
    display: none;
  }

  .cropper-select {
    padding-top: 6px;
  }

  .select-frame {
    width: 108px;
    height: 108px;
  }

  .cropper-select-hint {
    font-size: 9px;
  }
}
</style>

<!-- 全局样式：让 el-dialog 内部适应暗色 -->
<style>
.cropper-dialog {
  max-height: 92vh;
}

.cropper-dialog .el-dialog__body {
  max-height: calc(92vh - 150px);
  overflow-y: auto;
}
</style>
