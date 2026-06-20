<template>
  <div class="error-page">
    <div class="error-bg-decor" aria-hidden="true">
      <svg viewBox="0 0 600 600" xmlns="http://www.w3.org/2000/svg">
        <defs>
          <radialGradient id="glow-err" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stop-color="var(--accent-danger)" stop-opacity="0.12" />
            <stop offset="100%" stop-color="var(--accent-danger)" stop-opacity="0" />
          </radialGradient>
        </defs>
        <circle cx="300" cy="300" r="280" fill="url(#glow-err)" />
        <circle cx="300" cy="300" r="180" fill="none" stroke="var(--accent-danger)" stroke-opacity="0.06" stroke-width="1" />
        <circle cx="300" cy="300" r="120" fill="none" stroke="var(--accent-danger)" stroke-opacity="0.04" stroke-width="1" />
      </svg>
    </div>

    <div class="error-content">
      <div class="error-code">500</div>
      <h1 class="error-title">服务器错误</h1>
      <p class="error-desc">服务器暂时无法处理你的请求，请稍后重试</p>
      <router-link to="/">
        <el-button type="primary" class="error-btn" round>
          <i class="el-icon-back"></i>
          返回首页
        </el-button>
      </router-link>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ServerError'
}
</script>

<style scoped>
.error-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 56px - 48px);
  position: relative;
  overflow: hidden;
  animation: fade-in 0.4s var(--ease-out);
}

.error-bg-decor {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 600px;
  height: 600px;
  pointer-events: none;
  opacity: 0.8;
}

.error-bg-decor svg {
  width: 100%;
  height: 100%;
  animation: decor-spin 60s linear infinite;
}

.error-content {
  position: relative;
  z-index: 1;
  text-align: center;
  padding: 40px 24px;
}

.error-code {
  font-family: var(--font-display);
  font-size: 160px;
  line-height: 1;
  letter-spacing: 8px;
  color: var(--accent-danger);
  text-shadow:
    0 0 40px rgba(255, 77, 109, 0.25),
    0 0 80px rgba(255, 77, 109, 0.10);
  animation: code-pulse 3s ease-in-out infinite;
  user-select: none;
}

.error-title {
  font-family: var(--font-body);
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin-top: 16px;
  letter-spacing: 0.02em;
}

.error-desc {
  font-family: var(--font-body);
  font-size: 15px;
  color: var(--text-secondary);
  margin-top: 12px;
  max-width: 360px;
  margin-left: auto;
  margin-right: auto;
  line-height: 1.6;
}

.error-btn {
  margin-top: 32px;
  padding: 12px 32px !important;
  font-size: 15px !important;
  font-weight: 600 !important;
  background: var(--accent-primary) !important;
  border-color: var(--accent-primary) !important;
  color: var(--bg-page) !important;
  transition: all var(--duration-normal) var(--ease-out) !important;
}

.error-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 229, 160, 0.30) !important;
}

.error-btn i {
  margin-right: 6px;
}

@keyframes fade-in {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes code-pulse {
  0%, 100% {
    text-shadow:
      0 0 40px rgba(255, 77, 109, 0.25),
      0 0 80px rgba(255, 77, 109, 0.10);
  }
  50% {
    text-shadow:
      0 0 60px rgba(255, 77, 109, 0.35),
      0 0 120px rgba(255, 77, 109, 0.15);
  }
}

@keyframes decor-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@media (max-width: 767px) {
  .error-page {
    min-height: calc(100vh - 56px - 32px);
  }

  .error-code {
    font-size: 100px;
    letter-spacing: 4px;
  }

  .error-title {
    font-size: 20px;
  }

  .error-desc {
    font-size: 14px;
  }

  .error-bg-decor {
    width: 360px;
    height: 360px;
  }
}
</style>
