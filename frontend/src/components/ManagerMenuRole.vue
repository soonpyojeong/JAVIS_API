<template>
  <div class="role-menu-auth-manager">
    <div class="role-list">
      <h3>역할</h3>
      <ul>
        <li
          v-for="role in roleList"
          :key="role.roleId"
          :class="{ active: role.roleId === selectedRole?.roleId }"
          @click="selectRole(role)"
        >
          {{ role.roleName }}
        </li>
      </ul>
    </div>

    <div class="menu-list">
      <h3>메뉴</h3>
      <ul>
        <li
          v-for="menu in menuList"
          :key="menu.menuId"
          :class="{ selected: menu.menuId === selectedMenu?.menuId }"
          @click="selectMenu(menu)"
        >
          {{ menu.menuName }}
          <span v-if="menu.iconClass"><i :class="menu.iconClass"></i></span>
        </li>
      </ul>
    </div>

    <div class="auth-detail" v-if="selectedRole && selectedMenu">
      <h3>권한설정: {{ selectedRole.roleName }} - {{ selectedMenu.menuName }}</h3>
      <div>
        <label><input type="checkbox" v-model="authForm.canRead" true-value="Y" false-value="N" /> 읽기</label>
        <label><input type="checkbox" v-model="authForm.canWrite" true-value="Y" false-value="N" /> 쓰기</label>
        <label><input type="checkbox" v-model="authForm.canDelete" true-value="Y" false-value="N" /> 삭제</label>
      </div>
      <Button label="저장" @click="saveAuth" severity="primary" />
    </div>

    <!-- 👇 user-list 영역 DataTable로 교체 -->
    <div v-if="selectedRole" class="user-list">
      <h3>{{ selectedRole.roleName }} 역할 사용자</h3>
      <DataTable :value="usersForRole" :rows="5" paginator responsiveLayout="scroll"
                   :sortField="'loginId'"
                   :sortOrder="1" >
        <Column field="loginId" header="Login ID" sortable />
        <Column field="username" header="사용자명" sortable>
          <template #body="slotProps">
            {{ slotProps.data.username || '-' }}
          </template>
        </Column>
        <Column field="email" header="Email" />
        <Column header="User Role">
          <template #body="slotProps">
            <Dropdown
              v-model="slotProps.data.userRole"
              :options="roleOptions"
              optionLabel="label"
              optionValue="value"
              @change="onUserRoleChange(slotProps.data)"
              style="width: 120px"
            />
          </template>
        </Column>
      </DataTable>

    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import api from "@/api";
import Button from "primevue/button";
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
// 변수 선언
const roleList = ref([]);
const menuList = ref([]);
const selectedRole = ref(null);
const selectedRoleId = ref(null);
const selectedMenu = ref(null);
const usersForRole = ref([]);
const roleMenuAuthList = ref([]);
import Dropdown from 'primevue/dropdown'

const roleOptions = [
  { label: 'ADMIN', value: 'ADMIN', roleId: 1 },
  { label: 'DBA', value: 'DBA', roleId: 2 },
  { label: 'EAI', value: 'EAI', roleId: 3 },
  { label: 'VIEW', value: 'VIEW', roleId: 4 }
];


const authForm = ref({
  canRead: "N",
  canWrite: "N",
  canDelete: "N"
});

const authFormMap = ref({});




// 최초 로딩
onMounted(async () => {
  const rolesRes = await api.get("/api/auth/role/all");
  //console.log("rolesRes.data:", rolesRes.data);
  roleList.value = rolesRes.data;

  const menusRes = await api.get("/api/auth/menu/all");
  //console.log("menusRes.data:", menusRes.data);
  menuList.value = menusRes.data;
});

// 역할 선택 시 사용자 목록 로딩
const selectRole = async (role) => {
  selectedRole.value = role;
  selectedRoleId.value = role.roleId;

  selectedMenu.value = null; // ✅ 메뉴 선택 해제

  const res = await api.get(`/api/auth/role/${role.roleId}/users`);
  usersForRole.value = res.data;
};


const onUserRoleChange = async (user) => {
  const matched = roleOptions.find(r => r.value === user.userRole);
  if (!matched) return;

  try {
    await api.post('/api/auth/user-role/update', {
      userId: user.id,
      userRole: matched.value,    // JAVIS_LOGIN_USER.user_role
      roleId: matched.roleId      // TB_USER_ROLE.role_id
    });
    alert('사용자 역할이 변경되었습니다!');
  } catch (err) {
    console.error(err);
    alert('역할 변경 실패!');
  }
};

// 메뉴 선택 시 권한 정보 로딩
const selectMenu = async (menu) => {
  selectedMenu.value = menu;

  if (!selectedRole.value || !selectedMenu.value) return;

  const key = `${selectedRole.value.roleId}_${selectedMenu.value.menuId}`;
  if (authFormMap.value[key]) {
    authForm.value = { ...authFormMap.value[key] }; // 저장된 값 불러오기
  } else {
    const res = await api.get(`/api/auth/role-menu/auth?roleId=${selectedRole.value.roleId}&menuId=${menu.menuId}`);
    const { canRead, canWrite, canDelete } = res.data || {};
    authForm.value = {
      canRead: canRead || "N",
      canWrite: canWrite || "N",
      canDelete: canDelete || "N"
    };
    authFormMap.value[key] = { ...authForm.value }; // 새로 저장
  }
};


// 권한 초기화
const resetAuthForm = () => {
  authForm.value = {
    canRead: "N",
    canWrite: "N",
    canDelete: "N"
  };
};

// 저장
const saveAuth = async () => {
  if (!selectedRole.value || !selectedMenu.value) return;

  await api.post("/api/auth/role-menu/save", {
    roleId: selectedRole.value.roleId,
    menuId: selectedMenu.value.menuId,
    ...authForm.value
  });

  const key = `${selectedRole.value.roleId}_${selectedMenu.value.menuId}`;
  authFormMap.value[key] = { ...authForm.value }; // 저장 동기화

  alert("저장되었습니다!");
};


</script>

<style scoped>
.role-menu-auth-manager {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin: 40px auto;
  max-width: 1000px;
}
.role-list,
.menu-list {
  width: 200px;
  background: #f5f5f5;
  border-radius: 8px;
  padding: 15px;
}
.role-list ul,
.menu-list ul {
  list-style: none;
  padding: 0;
}
.role-list li,
.menu-list li {
  padding: 8px;
  cursor: pointer;
  border-radius: 4px;
}
.role-list li.active,
.menu-list li.selected {
  background: #007bff;
  color: #fff;
}
.auth-detail {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 0 10px #eee;
}
.auth-detail h3 {
  margin-bottom: 10px;
}
.auth-detail label {
  margin-right: 18px;
}
.user-list {
  width: 100%;
  margin-top: 20px;
  background: #f9f9f9;
  border-radius: 8px;
  padding: 15px;
}
</style>
