
<template>
  <div class="inlabel-datepicker-wrapper">
    <IftaLabel>
      <DatePicker
        v-model="localValue"
        :theme="{ preset: Aura }"
        showIcon
        inputId="collected-date"
        dateFormat="yy-mm-dd"
        panelClass="my-modern-calendar"
        size="small"
        @date-select="onDateSelected"
        @month-change="onMonthChange"
      >
        <template #date="slotProps">
          <span
            v-if="collectedDates && collectedDates.includes(
              slotProps.date.year + '-' +
              String(slotProps.date.month + 1).padStart(2, '0') + '-' +
              String(slotProps.date.day).padStart(2, '0'))"
            class="my-modern-collected"
          >
            {{ slotProps.date.day }}
          </span>
          <span v-else>
            {{ slotProps.date.day }}
          </span>
        </template>
      </DatePicker>
      <label for="collected-date">수집일</label>
    </IftaLabel>
  </div>
</template>


<script setup>
import { ref, watch } from 'vue'
import DatePicker from 'primevue/datepicker'
import IftaLabel from 'primevue/iftalabel'
import Aura from '@primeuix/themes/aura'
import '../scripts/custom-calendar.css'

const props = defineProps({
  modelValue: { type: [Date, String, null], default: null },
  collectedDates: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:modelValue', 'date-select', 'month-change'])

// 내부 값 관리용
const localValue = ref(props.modelValue)

watch(() => props.modelValue, (newVal) => {
  localValue.value = newVal
})

const onDateSelected = (date) => {
  emit('update:modelValue', date)
  emit('date-select', date)
}
const onMonthChange = (e) => emit('month-change', e)

</script>
