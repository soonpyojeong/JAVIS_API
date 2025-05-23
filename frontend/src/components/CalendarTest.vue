<template>
    <div class="card flex flex-wrap justify-center items-end gap-4">
        <IftaLabel>
            <DatePicker
             v-model="value"
             inputId="ifup"
             showIcon
             iconDisplay="input"
             variant="filled"
             size="small"
             dateFormat="yy-mm-dd"
             />
             <label for="ifup">수집일</label>
        </IftaLabel>
    </div>

</template>

<script setup>
import DatePicker from 'primevue/datepicker';
import IftaLabel from 'primevue/iftalabel';
import { ref } from "vue";

const value = ref(null);
</script>
