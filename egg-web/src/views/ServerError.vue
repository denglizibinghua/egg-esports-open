<template>
  <div class="error-page">
    <div class="error-grid" aria-hidden="true"></div>
    <div class="error-glow" aria-hidden="true"></div>

    <div class="error-frame">
      <div class="error-frame-head">
        <span class="error-eyebrow">// ERROR · SERVER FAULT</span>
        <span class="error-channel">CH-50</span>
      </div>

      <div class="error-code">500</div>

      <h1 class="error-title">服务器开小差</h1>
      <p class="error-desc">服务器暂时无法处理你的请求。稍等片刻后重试，或先返回首页继续浏览。</p>

      <div class="error-actions">
        <el-button type="primary" class="error-btn" @click="handleRetry">
          <el-icon><Refresh /></el-icon>
          重试
        </el-button>
        <router-link to="/">
          <el-button class="error-btn error-btn--ghost">
            <el-icon><Back /></el-icon>
            返回首页
          </el-button>
        </router-link>
      </div>

      <div class="error-meta">
        <span>EGG ESPORTS</span>
        <span class="meta-sep">/</span>
        <span>SIGNAL 500</span>
      </div>
    </div>
  </div>
</template>

<script>
// Element Plus 图标按需引入（用于模板中的 <Back /> / <Refresh /> 标签）
import { Back, Refresh } from '@element-plus/icons-vue'

export default {
  name: 'ServerError',
  components: { Back, Refresh },
  methods: {
    handleRetry() {
      this.$router.go(0)
    }
  }
}
</script>

<style scoped>
.error-page {
  position: relative;
  min-height: calc(100vh - var(--header-h) - 160px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 16px 64px;
  overflow: hidden;
}

/* Tactical grid backdrop */
.error-grid {
  position: absolute;
  inset: -15%;
  background-image:
    linear-gradient(var(--border-subtle) 1px, transparent 1px),
    linear-gradient(90deg, var(--border-subtle) 1px, transparent 1px);
  background-size: 56px 56px;
  mask-image: radial-gradient(ellipse 52% 54% at 50% 46%, #000 0%, transparent 76%);
  -webkit-mask-image: radial-gradient(ellipse 52% 54% at 50% 46%, #000 0%, transparent 76%);
  opacity: 0.85;
  pointer-events: none;
}

.error-glow {
  position: absolute;
  left: 50%;
  top: 44%;
  width: min(760px, 94vw);
  height: min(440px, 64vw);
  transform: translate(-50%, -50%);
  background: radial-gradient(ellipse at center, color-mix(in srgb, var(--accent-danger) 13%, transparent), transparent 68%);
  pointer-events: none;
}

.error-frame {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 620px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 30px 32px 24px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.02) 0%, transparent 50%),
    color-mix(in srgb, var(--bg-surface-1) 84%, transparent);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  box-shadow: var(--shadow-lg);
  animation: error-rise 0.45s var(--ease-out) both;
}

.error-frame::before,
.error-frame::after {
  content: '';
  position: absolute;
  width: 12px;
  height: 12px;
  border-color: color-mix(in srgb, var(--accent-danger) 38%, transparent);
  border-style: solid;
  pointer-events: none;
}

.error-frame::before {
  top: -1px;
  left: -1px;
  border-width: 1px 0 0 1px;
  border-top-left-radius: var(--radius-lg);
}

.error-frame::after {
  right: -1px;
  bottom: -1px;
  border-width: 0 1px 1px 0;
  border-bottom-right-radius: var(--radius-lg);
}

@keyframes error-rise {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.error-frame-head {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-subtle);
}

.error-eyebrow {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--accent-danger);
  text-align: left;
}

.error-channel {
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.14em;
  color: var(--text-faint);
}

.error-code {
  font-family: var(--font-display);
  font-size: clamp(132px, 24vw, 208px);
  line-height: 0.86;
  letter-spacing: 0.03em;
  color: var(--accent-danger);
  text-shadow:
    0 0 70px color-mix(in srgb, var(--accent-danger) 28%, transparent),
    0 0 160px color-mix(in srgb, var(--accent-danger) 12%, transparent);
  margin: 18px 0 6px;
  user-select: none;
}

.error-title {
  font-family: var(--font-body);
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.01em;
  line-height: 1.25;
  color: var(--text-primary);
}

.error-desc {
  font-size: 13.5px;
  color: var(--text-tertiary);
  margin-top: 10px;
  max-width: 400px;
  line-height: 1.75;
}

.error-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 26px;
}

.error-actions a {
  display: inline-flex;
}

.error-btn {
  height: 44px;
  padding: 0 22px;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.error-btn :deep(.el-icon) {
  margin-right: 6px;
  font-size: 14px;
}

.error-meta {
  width: 100%;
  margin-top: 26px;
  padding-top: 14px;
  border-top: 1px solid var(--border-subtle);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--text-faint);
}

.meta-sep {
  color: var(--border-strong);
}

@media (max-width: 767px) {
  .error-page {
    min-height: calc(100vh - var(--header-h) - 120px);
    padding: 24px 12px 48px;
  }

  .error-frame {
    padding: 24px 18px 20px;
  }

  .error-code {
    font-size: clamp(108px, 32vw, 150px);
    margin: 14px 0 4px;
  }

  .error-title {
    font-size: 21px;
  }

  .error-desc {
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .error-frame {
    padding: 20px 14px 18px;
  }

  .error-actions {
    flex-direction: column;
    width: 100%;
    max-width: 260px;
  }

  .error-actions a {
    width: 100%;
  }

  .error-btn {
    width: 100%;
    height: 46px;
  }

  .error-meta {
    margin-top: 20px;
    font-size: 9px;
    letter-spacing: 0.1em;
    flex-wrap: wrap;
    gap: 6px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .error-frame {
    animation: none;
  }
}
</style>
