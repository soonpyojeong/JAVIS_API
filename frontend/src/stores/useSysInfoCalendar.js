// ✅ useSysInfoCalendar.js
import { ref } from 'vue';
import api from '@/api';

export function useSysInfoCalendar() {
  const collectedDatesCache = new Map();
  const collectedDates = ref([]);
  const loadingCollectedDates = ref(false);

  const getCacheKey = (hostname, year, month) => `${hostname}_${year}-${String(month).padStart(2, '0')}`;

  const loadCollectedDates = async (hostname, year, month) => {
    const key = getCacheKey(hostname, year, month);

    console.log('🔎 API 호출 params:', { hostname, year, month });

    if (collectedDatesCache.has(key)) {
      collectedDates.value = collectedDatesCache.get(key);
      loadingCollectedDates.value = false;
      return;
    }

    loadingCollectedDates.value = true;

    try {
      const res = await api.get('/api/sysinfo/collected-dates-by-month', {
        params: { hostname, year, month }
      });
      console.log('🔎 API 응답:', res.data);

      const parsedDates = (Array.isArray(res.data) ? res.data : [])
        .filter(dateStr => /^\d{4}-\d{2}-\d{2}$/.test(dateStr));
      collectedDatesCache.set(key, parsedDates);
      collectedDates.value = parsedDates;
    } catch (err) {
      console.error(`❌ ${key} 수집일 조회 실패`, err);
      // 실패해도 기존 값 유지
    } finally {
      loadingCollectedDates.value = false;
    }
  };


  return {
    collectedDates,
    loadCollectedDates,
    loadingCollectedDates, // 필요시 외부에서도 쓸 수 있게!
  };
}
