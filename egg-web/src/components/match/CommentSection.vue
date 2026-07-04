<template>
  <!-- 评论区：评论列表、发表评论框、加载更多回复、WebSocket 实时推送 -->
  <div class="comments-panel">
    <h3 class="panel-title">
      <el-icon><ChatDotSquare /></el-icon>
      <span>评论</span>
      <span class="comment-count">{{ commentTotal }}</span>
    </h3>

    <!-- Not Logged In -->
    <div v-if="!getToken()" class="bet-login-prompt">
      <div class="prompt-icon">🔒</div>
      <p class="prompt-text">登录后参与评论</p>
      <el-button type="primary" class="prompt-btn" @click="$router.push('/login')">去登录</el-button>
    </div>

    <template v-else>
      <!-- Comment Input -->
      <div class="comment-input-wrap">
        <div v-if="replyTarget" class="reply-hint">
          <span>回复 @{{ replyTarget.userNickname }}</span>
          <el-button link size="mini" @click="cancelReply">取消</el-button>
        </div>
        <el-input
          v-model="commentInput"
          type="textarea"
          :rows="3"
          :placeholder="replyTarget ? '回复 @' + replyTarget.userNickname + '...' : '发表你的看法（支持 @昵称 提及）...'"
          maxlength="500"
          show-word-limit
        />
        <div class="comment-input-actions">
          <el-button
            type="primary"
            size="small"
            :loading="commentPosting"
            :disabled="!commentInput.trim()"
            @click="postComment"
          >发送</el-button>
        </div>
      </div>

      <!-- Comment List -->
      <div v-if="comments.length === 0 && !commentsLoading" class="comment-empty">
        <el-empty description="还没有评论，快来抢沙发" />
      </div>

      <div v-else class="comment-list">
        <div v-for="comment in comments" :key="comment.id" class="comment-item">
          <div class="comment-avatar">
            <el-image :src="comment.userAvatar" alt="" fit="cover">
              <template #error><span class="avatar-placeholder">{{ (comment.userNickname || '?').charAt(0) }}</span></template>
            </el-image>
          </div>
          <div class="comment-body">
            <div class="comment-header">
              <span class="comment-nickname">{{ comment.userNickname || '匿名用户' }}</span>
              <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
            </div>
            <div class="comment-content" v-html="renderContent(comment.content)"></div>
            <div class="comment-actions">
              <el-button link size="mini" @click="startReply(comment)">回复</el-button>
              <el-button
                v-if="currentUser && comment.userId === currentUser.id"
                link
                size="mini"
                class="delete-btn"
                @click="deleteComment(comment)"
              >删除</el-button>
            </div>

            <!-- Nested Replies -->
            <div v-if="comment.replies && comment.replies.length > 0" class="reply-list">
              <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
                <div class="comment-avatar comment-avatar--sm">
                  <el-image :src="reply.userAvatar" alt="" fit="cover">
                    <template #error><span class="avatar-placeholder">{{ (reply.userNickname || '?').charAt(0) }}</span></template>
                  </el-image>
                </div>
                <div class="comment-body">
                  <div class="comment-header">
                    <span class="comment-nickname">{{ reply.userNickname || '匿名用户' }}</span>
                    <span v-if="reply.replyToNickname" class="reply-to">回复 @{{ reply.replyToNickname }}</span>
                    <span class="comment-time">{{ formatTime(reply.createTime) }}</span>
                  </div>
                  <div class="comment-content" v-html="renderContent(reply.content)"></div>
                  <div class="comment-actions">
                    <el-button link size="mini" @click="startReply(reply, comment)">回复</el-button>
                    <el-button
                      v-if="currentUser && reply.userId === currentUser.id"
                      link
                      size="mini"
                      class="delete-btn"
                      @click="deleteComment(reply)"
                    >删除</el-button>
                  </div>
                </div>
              </div>
              <el-button
                v-if="comment.replies.length < comment.replyTotal"
                link
                size="mini"
                class="load-more-replies"
                @click="loadMoreReplies(comment)"
              >展开更多回复 ({{ comment.replyTotal - comment.replies.length }})</el-button>
            </div>
          </div>
        </div>
      </div>

      <el-pagination
        v-if="commentTotal > commentPageSize"
        background
        layout="prev, pager, next"
        :total="commentTotal"
        :page-size="commentPageSize"
        v-model:current-page="commentPage"
        @current-change="fetchComments"
        class="comment-pagination"
      />
    </template>
  </div>
</template>

<script>
import { markRaw } from 'vue'
import request from '@/utils/request'
// 分页数据提取工具（兼容 records/list/content），统一提取自 @/utils/pagination
import { extractList, extractTotal } from '@/utils/pagination'
import { getToken } from '@/utils/auth'
import { subscribe as wsSubscribe, unsubscribe as wsUnsubscribe } from '@/utils/websocket'
import DOMPurify from 'dompurify'
// Element Plus SVG 图标（迁移自 Element UI 字体图标）
import { ChatDotSquare } from '@element-plus/icons-vue'

export default {
  name: 'CommentSection',
  props: {
    // 比赛 ID，用于加载评论和订阅 WebSocket
    matchId: { type: [String, Number], required: true },
    // 当前登录用户，用于判断是否显示删除按钮
    currentUser: { type: Object, default: null }
  },
  data() {
    return {
      // 暴露图标组件供模板绑定使用（markRaw 避免被响应式化）
      ChatDotSquare: markRaw(ChatDotSquare),
      // 评论列表
      comments: [],
      commentsLoading: false,
      commentTotal: 0,
      commentPage: 1,
      commentPageSize: 20,
      // 评论输入
      commentInput: '',
      commentPosting: false,
      // 回复目标（replyTarget=被回复对象，replyParent=顶级父评论）
      replyTarget: null,
      replyParent: null
    }
  },
  mounted() {
    // 加载评论列表
    this.fetchComments()
    // 订阅评论 WebSocket（实时推送新评论）
    if (this.matchId) {
      wsSubscribe('/topic/match/' + this.matchId, this.onWsComment)
    }
  },
  beforeUnmount() {
    // 取消订阅评论 WebSocket，避免连接泄漏
    if (this.matchId) {
      wsUnsubscribe('/topic/match/' + this.matchId)
    }
  },
  methods: {
    // 暴露给模板判断登录态
    getToken() {
      return getToken()
    },
    // 格式化时间为 YYYY-MM-DD HH:mm
    formatTime(timeStr) {
      if (!timeStr) return ''
      var d = new Date(timeStr)
      var y = d.getFullYear()
      var month = (d.getMonth() + 1).toString().padStart(2, '0')
      var day = d.getDate().toString().padStart(2, '0')
      var hour = d.getHours().toString().padStart(2, '0')
      var min = d.getMinutes().toString().padStart(2, '0')
      return y + '-' + month + '-' + day + ' ' + hour + ':' + min
    },
    // 加载评论列表（分页）
    fetchComments(silent) {
      if (!silent) this.commentsLoading = true
      request.get('/comment/list', {
        params: {
          matchId: this.matchId,
          page: this.commentPage,
          size: this.commentPageSize
        }
      }).then(function (res) {
        var data = res.data || res
        // 兼容 records/list/content 多种后端返回格式
        var records = extractList(data)
        // 填充 replyTotal（后端未返回时默认为 replies 长度）
        records.forEach(function (c) {
          if (c.replyTotal == null) c.replyTotal = (c.replies || []).length
        })
        this.comments = records
        // 提取分页总数（仅读取 data.total）
        this.commentTotal = extractTotal(data)
      }.bind(this)).catch(() => { this.$message.error('加载失败') }).finally(function () {
        this.commentsLoading = false
      }.bind(this))
    },
    // 发表评论（或回复）
    postComment() {
      if (!this.commentInput.trim()) return
      this.commentPosting = true
      var body = {
        matchId: parseInt(this.matchId),
        content: this.commentInput.trim()
      }
      // 回复场景：带上 parentId / replyToId / replyToUid
      if (this.replyTarget) {
        body.parentId = (this.replyParent || this.replyTarget).id
        body.replyToId = this.replyTarget.id
        body.replyToUid = this.replyTarget.userId
      }
      request.post('/comment/post', body).then(function () {
        this.$message.success('评论成功')
        this.commentInput = ''
        this.cancelReply()
        this.fetchComments(true)
      }.bind(this)).catch(function () {}).finally(function () {
        this.commentPosting = false
      }.bind(this))
    },
    // 开始回复某条评论（comment=被回复对象，parent=顶级父评论）
    startReply(comment, parent) {
      this.replyTarget = comment
      this.replyParent = parent || null
    },
    // 取消回复
    cancelReply() {
      this.replyTarget = null
      this.replyParent = null
    },
    // 删除评论（带确认弹窗）
    deleteComment(comment) {
      this.$confirm('确定删除这条评论？', '确认删除', {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(function () {
        request.delete('/comment/' + comment.id).then(function () {
          this.$message.success('删除成功')
          this.fetchComments(true)
        }.bind(this)).catch(function () {})
      }.bind(this)).catch(function () {})
    },
    // 加载更多回复
    loadMoreReplies(comment) {
      request.get('/comment/replies', {
        params: { parentId: comment.id, page: 1, size: 100 }
      }).then(function (res) {
        var data = res.data || res
        // 兼容 records/list/content 多种后端返回格式
        var records = extractList(data)
        comment.replies = records
        // 保持 replyTotal 为后端返回的总数，不覆盖
      }.bind(this)).catch(function () {})
    },
    // 渲染评论内容（XSS 防御 + @提及高亮）
    renderContent(content) {
      if (!content) return ''
      // 先用 DOMPurify 剥掉所有 HTML 标签（XSS 防御）
      var cleaned = DOMPurify.sanitize(content, { ALLOWED_TAGS: [] })
      // @提及高亮
      return cleaned.replace(/@(\S+)/g, '<span class="at-mention">@$1</span>')
    },
    // WebSocket 收到新评论的回调
    onWsComment(newComment) {
      if (!newComment) return
      if (newComment.parentId) {
        // 回复：找到父评论 push 到 replies
        var parent = this.comments.find(function (c) { return c.id === newComment.parentId })
        if (parent) {
          if (!parent.replies) parent.replies = []
          parent.replies.push(newComment)
          parent.replyTotal = (parent.replyTotal || 0) + 1
        }
      } else {
        // 顶级评论：unshift 到列表顶
        if (!newComment.replies) newComment.replies = []
        if (newComment.replyTotal == null) newComment.replyTotal = 0
        this.comments.unshift(newComment)
        this.commentTotal++
      }
    }
  }
}
</script>

<style scoped>
/* Comments Panel */
.comments-panel {
  background: rgba(18,18,26,0.65);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-xl);
  padding: 28px;
  margin-top: 24px;
}

.panel-title {
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 22px;
  letter-spacing: 1px;
  margin-bottom: 20px;
  border-bottom: 1px solid var(--border-subtle);
  padding-bottom: 12px;
}

.comments-panel .panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.comments-panel .panel-title i {
  color: var(--accent-primary);
  font-size: 20px;
}

.comment-count {
  color: var(--text-tertiary);
  font-size: 14px;
  font-family: var(--font-mono);
  font-weight: 400;
  margin-left: 4px;
}

.comment-input-wrap {
  margin-bottom: 24px;
}

.reply-hint {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  margin-bottom: 8px;
  background: var(--bg-surface-3);
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: var(--text-secondary);
}

.comment-input-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.comment-input-actions .el-button--primary {
  background: var(--accent-primary) !important;
  border-color: var(--accent-primary) !important;
  color: #0a0a0f !important;
  font-weight: 600;
}

.comment-empty {
  padding: 24px 0;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.comment-item {
  display: flex;
  gap: 12px;
}

.comment-avatar {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  overflow: hidden;
  background: var(--bg-surface-3);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.comment-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.comment-avatar--sm {
  width: 28px;
  height: 28px;
}

.avatar-placeholder {
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 600;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.comment-nickname {
  color: var(--text-primary);
  font-weight: 600;
  font-size: 13px;
}

.reply-to {
  color: var(--text-tertiary);
  font-size: 12px;
}

.comment-time {
  color: var(--text-tertiary);
  font-size: 12px;
  margin-left: auto;
}

.comment-content {
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.comment-content :deep(.at-mention) {
  color: var(--accent-primary);
  font-weight: 500;
}

.comment-actions {
  display: flex;
  gap: 8px;
  margin-top: 4px;
}

.comment-actions .el-button--text {
  color: var(--text-tertiary) !important;
  font-size: 12px;
  padding: 2px 0;
}

.comment-actions .el-button--text:hover {
  color: var(--accent-primary) !important;
}

.comment-actions .delete-btn:hover {
  color: var(--accent-danger) !important;
}

.reply-list {
  margin-top: 12px;
  padding: 12px;
  background: var(--bg-surface-3);
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.reply-item {
  display: flex;
  gap: 8px;
}

.load-more-replies {
  color: var(--accent-secondary) !important;
  font-size: 12px !important;
  padding: 4px 0 !important;
}

.load-more-replies:hover {
  color: var(--accent-primary) !important;
}

.comment-pagination {
  margin-top: 20px;
  text-align: center;
}

/* Bet Login Prompt（评论未登录提示复用） */
.bet-login-prompt {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 24px;
  text-align: center;
}

.prompt-icon {
  font-size: 36px;
  margin-bottom: 12px;
  opacity: 0.7;
}

.prompt-text {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 16px;
}

.prompt-btn {
  background: var(--accent-primary) !important;
  border-color: var(--accent-primary) !important;
  color: #0a0a0f !important;
  font-weight: 600;
  border-radius: var(--radius-md) !important;
  padding: 10px 32px;
}

/* ===== 移动端适配 ===== */
@media (max-width: 768px) {
  /* 评论面板：缩小内边距 */
  .comments-panel {
    padding: 18px 14px;
    margin-top: 16px;
  }
  .comment-input-actions {
    justify-content: stretch;
  }
  .comment-input-actions .el-button {
    flex: 1;
    min-height: 40px;
  }

  /* 未登录提示按钮：全宽 */
  .bet-login-prompt {
    padding: 24px 16px;
  }
  .prompt-btn {
    width: 100%;
    min-height: 44px;
  }
}
</style>
