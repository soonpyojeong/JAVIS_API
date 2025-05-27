<template>
  <Dialog
    v-model:visible="visible"
    modal
    :header="mode === 'edit' ? 'DB 수정' : 'DB 등록'"
    :closable="false"
    style="width: 400px; max-width: 95vw;"
    @hide="onCancel"
  >
    <form @submit.prevent="onSubmit">
      <div class="flex flex-col gap-3">
        <Select
          v-model="localForm.loc"
          :options="locOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="지역 선택"
          class="w-full"
          :class="{ 'p-invalid': showError && !localForm.loc }"
          appendTo="body"
        />
        <small v-if="showError && !localForm.loc" class="p-error">V</small>

        <Select
          v-model="localForm.dbType"
          :options="dbTypeOptions"
          @change="onDbTypeChange"
          optionLabel="label"
          optionValue="value"
          placeholder="DB 타입 선택"
          class="w-full"
          :class="{ 'p-invalid': showError && !localForm.dbType }"
          appendTo="body"
        />
        <small v-if="showError && !localForm.dbType" class="p-error">V</small>

        <Select
          v-model="localForm.assets"
          :options="assetsOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="자산 선택"
          class="w-full"
          :class="{ 'p-invalid': showError && !localForm.assets }"
          appendTo="body"
        />
        <small v-if="showError && !localForm.assets" class="p-error">V</small>

        <InputText v-model="localForm.dbVer" placeholder="DB버전" class="w-full" />
        <InputText v-model="localForm.smsGroup" placeholder="SMS Group" class="w-full" />
        <InputText v-model="localForm.dbDescript" placeholder="설명" class="w-full" />

        <InputText
          v-model="localForm.os"
          placeholder="OS"
          class="w-full"
        />
        <InputText
          v-model="localForm.hostname"
          placeholder="호스트명"
          class="w-full"
          :class="{ 'p-invalid': showError && !localForm.hostname }"
        />
        <small v-if="showError && !localForm.hostname" class="p-error">V</small>

        <InputText
          v-model="localForm.dbName"
          placeholder="DB 이름"
          class="w-full"
          :class="{ 'p-invalid': showError && !localForm.dbName }"
        />
        <small v-if="showError && !localForm.dbName" class="p-error">V</small>

        <InputText
          v-model="localForm.instanceName"
          placeholder="Instance Name"
          class="w-full"
          :class="{ 'p-invalid': showError && !localForm.instanceName }"
        />
        <small v-if="showError && !localForm.instanceName" class="p-error">V</small>

        <InputText v-model="localForm.pubIp" placeholder="PubIP" class="w-full"
         :class="{ 'p-invalid': showError && !localForm.PubIP }"/>
         <small v-if="showError && !localForm.PubIP" class="p-error">V</small>
        <InputText v-model="localForm.vip" placeholder="VIP" class="w-full" />
        <InputNumber
          v-model="localForm.port"
          placeholder="포트"
          class="w-full"
          :class="{ 'p-invalid': showError && !localForm.port }"
        />
        <small v-if="showError && !localForm.port" class="p-error">V</small>
        <InputText
          v-model="localForm.userid"
          placeholder="USERID"
          class="w-full"
          :class="{ 'p-invalid': showError && !localForm.userid }"
        />
        <small v-if="showError && !localForm.userid" class="p-error">V</small>

        <Password
          v-model="localForm.pw"
          placeholder="PW"
          class="w-full"
          :class="{ 'p-invalid': showError && !localForm.pw }"
          :feedback="false"
          toggleMask
        />
        <small v-if="showError && !localForm.pw" class="p-error">V</small>
      </div>

      <div v-if="dbCheckItems.length" class="flex flex-col gap-2 mt-3">
        <span class="font-semibold">모니터링 항목</span>
        <div v-for="item in dbCheckItems" :key="item.key" class="flex items-center gap-2">
          <Checkbox
            v-model="localForm[item.key]"
            binary
            trueValue="Y"
            falseValue="N"
            :inputId="item.key"
          />
          <label :for="item.key">{{ item.label }}</label>
        </div>
      </div>
      <br>
      <div class="flex justify-end gap-2">
        <Button :label="mode === 'edit' ? '저장' : '등록'" type="submit" severity="primary" class="font-bold" />
        <Button label="취소" type="button" severity="secondary" outlined @click="onCancel" />
      </div>
    </form>
  </Dialog>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import Dialog from 'primevue/dialog'
import Select from 'primevue/select'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Password from 'primevue/password'
import Checkbox from 'primevue/checkbox'
import Button from 'primevue/button'

const showError = ref(false)


function onDbTypeChange() {
  emit('dbTypeChange', localForm.value.dbType)
}
const props = defineProps({
  visible: Boolean,
  mode: String,
  form: Object, // 부모에서 :form="editForm"으로 내려주는 원본(ref/reactive)
  locOptions: Array,
  dbTypeOptions: Array,
  assetsOptions: Array,
  dbCheckItems: Array,
})
const emit = defineEmits(['update:visible', 'submit', 'cancel','dbTypeChange'  ])
const visible = computed({
  get: () => props.visible,
  set: v => emit('update:visible', v),
})

// localForm은 항상 복사본!
// (참고: form 필드가 ref/reactive라도 localForm이 복사이므로 원본 영향 X)
const localForm = ref({ ...props.form })


// props.form이 바뀌면(등록/수정 다이얼로그마다) localForm도 다시 복사
watch(
  () => props.form,
  (newForm) => {
    localForm.value = { ...newForm }
  },
  { immediate: true, deep: true }
)

// (아래처럼 사용자가 dbType을 바꾸면, localForm.dbType이 바뀌지만 부모는 영향 없음!)
// dbCheckItems는 여전히 부모에서 내려온 computed 값 사용
// 만약 dbType 바뀔 때 체크박스 동적 구성도 하고 싶으면, 부모에서 computed로 처리

function onSubmit() {
  showError.value = true
  if (
    !localForm.value.loc ||
    !localForm.value.dbType ||
    !localForm.value.assets ||
    !localForm.value.hostname ||
    !localForm.value.dbName ||
    !localForm.value.instanceName ||
    !localForm.value.userid ||
    !localForm.value.PubIP ||
    !localForm.value.port||
    !localForm.value.pw

  ) {
    // 필수값 누락시 에러메시지 표시만 하고 제출 안함
    return
  }
  emit('submit', { ...localForm.value })
}

function onCancel() {
  emit('update:visible', false)
  emit('cancel')
}
</script>
