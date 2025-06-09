<template>
  <div class="top-nav">
    <ul v-if="isLoggedIn">
       <li
              v-for="item in filteredMenuItems"
              :key="item.path"
              :class="{ active: selectedMenu === item.path }"
              @click="handleMenuClick(item)"
            >
              <i :class="item.iconClass + ' mr-2'"></i>
              {{ item.name }}
              <span v-if="item.name === '테이블스페이스관리'" style="margin-left: 5px;">▼</span>
              <span v-if="item.name === '일일점검'" style="margin-left: 5px;">▼</span>
              <!-- ETL DB관리면 ▼ 표시 -->
              <span v-if="item.name === 'ETL DB관리'" style="margin-left: 5px;">▼</span>
            </li>
      <li v-if="user && user.username" class="user-info-wrapper">
        <div class="user-info-badge" @click="toggleProfile">
          <span class="emoji">{{ roleEmoji }}</span>
          <span class="username">{{ user.username }}님</span>
          <span class="role">({{ user.userRole }})</span>
        </div>
        <div v-if="showProfile" class="profile-card">
          <div class="card-inner">
            <div class="card-header">
              <span class="emoji-big">{{ roleEmoji }}</span>
              <div>
                <div class="name">{{ user.username }}</div>
                <div class="email">{{ user.email || "이메일 없음" }}</div>
                <div class="role">🏷 {{ user.userRole }}</div>
              </div>
            </div>
            <hr />
            <button class="logout-card-btn" @click="logout">🚪 로그아웃</button>
          </div>
        </div>
        <div class="notification-wrapper">
        <div class="bell-icon" :class="{ active: hasUnread }" @click="toggleModal">
        🔔
          <span v-if="hasUnread" class="badge">{{ unreadCount }}</span>
         </div>
         <div v-if="showModal" class="alert-modal-below-nav">
         <div class="alert-modal">
             <h3 class="modal-title">📢 알림 내역</h3>
             <ul class="alert-list">
              <li v-for="alert in sortedAlerts" :key="alert.id" class="alert-item">
                <div>
                 <span class="timestamp">{{ formatDate(alert.createdAt) }}</span><br>
                  <span class="message">{{ alert.message }}</span>
                </div>
                 <button class="delete-btn" @click="dismissAlert(alert)">삭제</button>
                </li>
              </ul>
          </div>
         </div>
        </div>
      </li>
    </ul>

    <div v-else style="color: white;">😆 로그인 상태가 아닌다</div>
      <div
        v-if="showEtlSubMenu"
        class="etl-submenu"
      >
        <ul>
          <li
            v-for="sub in etlSubMenus"
            :key="sub.path"
            :class="{ active: selectedMenu === sub.path }"
            @click="navigateTo(sub.path)"
          >
            <i :class="sub.iconClass + ' mr-2'"></i>
            {{ sub.name }}
          </li>
        </ul>
      </div>
        <!-- 일일점검 드롭다운 -->
          <div v-if="showDailySubMenu" class="daily-submenu">
            <ul>
              <li
                v-for="sub in dailySubMenus"
                :key="sub.path"
                :class="{ active: selectedMenu === sub.path }"
                @click="navigateToSub(sub.path, 'daily')"
              >
                <i :class="sub.iconClass + ' mr-2'"></i>
                {{ sub.name }}
              </li>
            </ul>
          </div>
      <!-- 테이블스페이스관리 드롭다운 -->
      <div v-if="showSpaceSubMenu" class="space-submenu">
        <ul>
          <li
            v-for="sub in SpaceSubMenus"
            :key="sub.path"
            :class="{ active: selectedMenu === sub.path }"
            @click="navigateToSub(sub.path, 'space')"
          >
            <i :class="sub.iconClass + ' mr-2'"></i>
            {{ sub.name }}
          </li>
        </ul>
      </div>
  </div>
</template>