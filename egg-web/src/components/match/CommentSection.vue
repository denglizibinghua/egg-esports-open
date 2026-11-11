<template>
  <!-- 评论区：评论列表、发表评论框、加载更多回复、WebSocket 实时推送 -->
  <div class="comments-panel u-panel card-entrance">
    <header class="comments-head">
      <div class="comments-head-main">
        <span class="u-eyebrow">COMMENTS</span>
        <h3 class="comments-title">
          <el-icon class="comments-icon"><ChatDotSquare /></el-icon>
          <span>评论</span>
          <span class="comment-count">{{ commentTotal }}</span>
        </h3>
      </div>
      <span class="comments-meta">实时讨论</span>
    </header>

    <!-- Not Logged In -->
    <div v-if="!getToken()" class="state-panel state-panel--auth">
      <div class="state-head">
        <span class="state-code">AUTH</span>
        <p class="state-title">登录后参与评论</p>
      </div>
      <p class="state-desc">登录后可发表评论、回复其他玩家</p>
      <el-button type="primary" class="prompt-btn" @click="$router.push('/login')">去登录</el-button>
    </div>

    <template v-else>
      <!-- Comment Input -->
      <div class="comment-input-wrap">
        <div v-if="replyTarget" class="reply-hint">
          <span class="reply-hint-text">回复 <b>@{{ replyTarget.userNickname }}</b></span>
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
          <span class="composer-meta">支持 @昵称 提及</span>
          <el-button
            type="primary"
            size="small"
            class="comment-submit"
            :loading="commentPosting"
            :disabled="!commentInput.trim()"
            @click="postComment"
          >发送</el-button>
        </div>
      </div>

      <!-- Comment List -->
      <div v-if="comments.length === 0 && !commentsLoading" class="comment-empty">
        <span class="empty-mark">//</span>
        <p class="empty-title">还没有评论</p>
        <p class="empty-desc">快来抢沙发，聊聊这场比赛</p>
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
              <span v-if="currentUser && comment.userId === currentUser.id" class="comment-self">我</span>
              <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
            </div>
            <div class="comment-content" v-html="renderContent(comment.content)"></div>
            <div class="comment-actions">
              <el-button link size="mini" @click="startReply(comment)">回复</el-button>
              <span v-if="currentUser && comment.userId === currentUser.id" class="action-sep">/</span>
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
                    <span v-if="currentUser && reply.userId === currentUser.id" class="action-sep">/</span>
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
/* ===== Panel shell ===== */
.comments-panel {
  --panel-pad: 22px;
  position: relative;
  max-width: 880px;
  margin: 24px auto 0;
  padding: var(--panel-pad);
}

.comments-head {
  position: relative;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-subtle);
}

.comments-head::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: -1px;
  width: 44px;
  height: 2px;
  background: var(--brand);
}

.comments-head-main {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.comments-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-body);
  font-size: 24px;
  font-weight: 800;
  letter-spacing: -0.02em;
  line-height: 1.1;
  color: var(--text-primary);
}

.comments-icon {
  font-size: 18px;
  color: var(--text-tertiary);
}

.comment-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 26px;
  height: 20px;
  padding: 0 7px;
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: var(--text-secondary);
  background: var(--bg-surface-2);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
}

.comments-meta {
  font-family: var(--font-mono);
  font-size: 9.5px;
  font-weight: 600;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--text-faint);
  white-space: nowrap;
  flex-shrink: 0;
}

/* ===== Auth state ===== */
.state-panel {
  margin-top: 18px;
  padding: 18px;
  background: var(--bg-surface-2);
  border: 1px solid var(--border-subtle);
  border-left: 2px solid var(--brand);
  border-radius: var(--radius-md);
}

.state-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.state-code {
  display: inline-flex;
  align-items: center;
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.18em;
  color: var(--brand);
  border: 1px solid var(--brand);
  border-radius: var(--radius-sharp);
  padding: 2px 6px;
  flex-shrink: 0;
  text-transform: uppercase;
  line-height: 1.4;
}

.state-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.4;
}

.state-desc {
  font-size: 13px;
  color: var(--text-tertiary);
  line-height: 1.55;
}

.prompt-btn {
  margin-top: 14px;
  min-width: 108px;
  height: 36px;
  font-weight: 700;
}

/* ===== Composer ===== */
.comment-input-wrap {
  margin-top: 18px;
  margin-bottom: 20px;
}

.reply-hint {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 12px;
  margin-bottom: 8px;
  background: var(--bg-surface-2);
  border-left: 2px solid var(--brand);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
}

.reply-hint-text {
  font-size: 12.5px;
  color: var(--text-secondary);
  min-width: 0;
  overflow-wrap: anywhere;
}

.reply-hint-text b {
  font-weight: 700;
  color: var(--brand);
}

.reply-hint .el-button {
  flex-shrink: 0;
  font-size: 11.5px;
}

.comment-input-wrap :deep(.el-textarea__inner) {
  padding: 12px 14px 26px;
  font-family: var(--font-body);
  font-size: 13.5px;
  line-height: 1.65;
  border-radius: var(--radius-sm);
  resize: vertical;
}

.comment-input-wrap :deep(.el-input__count) {
  right: 12px;
  bottom: 8px;
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 10px;
  letter-spacing: 0.04em;
  color: var(--text-faint);
  background: transparent;
}

.comment-input-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 8px;
}

.composer-meta {
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--text-faint);
}

.comment-submit {
  min-width: 84px;
  height: 34px;
  font-size: 12.5px;
  font-weight: 700;
  letter-spacing: 0.1em;
}

/* ===== Empty ===== */
.comment-empty {
  padding: 32px 20px;
  text-align: center;
  border: 1px dashed var(--border-default);
  border-radius: var(--radius-md);
  background: var(--bg-inset);
}

.empty-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  margin-bottom: 10px;
  font-family: var(--font-mono);
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: var(--brand);
  background: var(--brand-soft);
  border: 1px solid var(--brand-line);
  border-radius: var(--radius-sm);
}

.empty-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
}

.empty-desc {
  margin-top: 3px;
  font-size: 12.5px;
  color: var(--text-tertiary);
}

/* ===== List ===== */
.comment-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: 10px 12px;
  margin: 0 -12px;
  border-radius: var(--radius-sm);
  transition: background var(--duration-fast) ease;
}

.comment-item:hover {
  background: var(--bg-surface-2);
}

.comment-avatar {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  border: 1px solid var(--border-subtle);
  background: var(--bg-surface-3);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
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
  font-family: var(--font-display);
  font-size: 16px;
  line-height: 1;
  color: var(--text-tertiary);
  letter-spacing: 0.02em;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 7px;
  min-height: 18px;
}

.comment-nickname {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.4;
  overflow-wrap: anywhere;
}

.comment-self {
  display: inline-flex;
  align-items: center;
  padding: 0 4px;
  font-size: 9px;
  font-weight: 700;
  line-height: 1.5;
  color: var(--brand);
  border: 1px solid var(--brand-line);
  border-radius: var(--radius-sharp);
  flex-shrink: 0;
}

.reply-to {
  font-size: 11px;
  color: var(--text-tertiary);
  overflow-wrap: anywhere;
}

.comment-time {
  margin-left: auto;
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 9.5px;
  letter-spacing: 0.03em;
  color: var(--text-faint);
  white-space: nowrap;
  flex-shrink: 0;
}

.comment-content {
  margin-top: 3px;
  font-size: 14px;
  line-height: 1.65;
  color: var(--text-secondary);
  overflow-wrap: anywhere;
  word-break: break-word;
}

.comment-content :deep(.at-mention) {
  color: var(--brand);
  font-weight: 600;
}

/* ===== Actions ===== */
.comment-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  margin-top: 5px;
}

.comment-actions .el-button {
  height: auto;
  padding: 0;
  font-size: 11.5px;
  font-weight: 600;
  color: var(--text-tertiary);
}

.comment-actions .el-button:hover {
  color: var(--text-primary);
}

.comment-actions .delete-btn:hover {
  color: var(--accent-danger) !important;
}

.action-sep {
  font-family: var(--font-mono);
  font-size: 10px;
  color: var(--text-faint);
}

/* ===== Replies ===== */
.reply-list {
  margin-top: 10px;
  padding-left: 14px;
  border-left: 1px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.reply-item {
  display: flex;
  gap: 10px;
  padding: 8px 10px;
  margin: 0 -10px;
  border-radius: var(--radius-sm);
  transition: background var(--duration-fast) ease;
}

.reply-item:hover {
  background: var(--bg-surface-2);
}

.reply-item .comment-content {
  font-size: 13.5px;
}

.reply-item .comment-nickname {
  font-size: 12.5px;
}

.load-more-replies {
  align-self: flex-start;
  margin-left: 2px;
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.04em;
  color: var(--text-tertiary);
}

.load-more-replies:hover {
  color: var(--brand);
}

/* ===== Pagination ===== */
.comment-pagination {
  margin-top: 20px;
  text-align: center;
}

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .comments-panel {
    --panel-pad: 16px;
    margin-top: 16px;
  }

  .comments-title {
    font-size: 21px;
  }

  .comments-meta {
    display: none;
  }

  .comment-item {
    padding: 9px 8px;
    margin: 0 -8px;
    gap: 10px;
  }

  .reply-item {
    padding: 7px 8px;
    margin: 0 -8px;
  }

  .comment-input-actions .el-button {
    min-height: 36px;
  }
}

@media (max-width: 480px) {
  .comments-panel {
    --panel-pad: 12px;
  }

  .comments-title {
    font-size: 19px;
  }

  .comment-count {
    height: 18px;
    min-width: 22px;
    font-size: 9.5px;
  }

  .comment-input-wrap {
    margin-top: 14px;
    margin-bottom: 16px;
  }

  .comment-content {
    font-size: 13.5px;
  }

  .comment-time {
    font-size: 9px;
  }

  .comment-avatar {
    width: 32px;
    height: 32px;
  }

  .comment-avatar--sm {
    width: 24px;
    height: 24px;
  }

  .avatar-placeholder {
    font-size: 14px;
  }

  .reply-list {
    padding-left: 10px;
  }

  .comment-empty {
    padding: 26px 14px;
  }

  .comment-input-actions {
    gap: 8px;
  }

  .comment-submit {
    min-width: 72px;
  }

  .composer-meta {
    font-size: 9px;
    letter-spacing: 0.06em;
  }
}
</style>
