<template>
  <div class="dashboard-tab-wrapper">
    <TabView>
      <TabPanel header="포항DMES Collector">
        <div class="p-3">
          <iframe :key="paperPHBootUrl" :src="paperPHBootUrl"
                  frameborder="0" style="width:100%;height:calc(100vh - 140px)" />
        </div>
      </TabPanel>

      <TabPanel header="당진DMES Collector">
        <div class="p-3">
          <iframe :key="paperDJBootUrl" :src="paperDJBootUrl"
                  frameborder="0" style="width:100%;height:calc(100vh - 140px)" />
        </div>
      </TabPanel>

      <TabPanel header="인천DMES Collector">
        <div class="p-3">
          <iframe :key="paperICBootUrl" :src="paperICBootUrl"
                  frameborder="0" style="width:100%;height:calc(100vh - 140px)" />
        </div>
      </TabPanel>

      <TabPanel header="냉연MES Collector">
        <div class="p-3">
          <iframe :key="paperCMBootUrl" :src="paperCMBootUrl"
                  frameborder="0" style="width:100%;height:calc(100vh - 140px)" />
        </div>
      </TabPanel>

      <TabPanel header="IRIS Collector">
        <div class="p-3">
          <iframe :key="paperIRISBootUrl" :src="paperIRISBootUrl"
                  frameborder="0" style="width:100%;height:calc(100vh - 140px)" />
        </div>
      </TabPanel>

      <TabPanel header="DBOX Collector">
        <div class="p-3">
          <iframe :key="paperDBOXBootUrl" :src="paperDBOXBootUrl"
                  frameborder="0" style="width:100%;height:calc(100vh - 140px)" />
        </div>
      </TabPanel>
    </TabView>
  </div>
</template>

<script setup>
import { computed } from 'vue'

// 네가 고정하기로 한 URL (그대로 사용)
const paperPHUrl   = '/paper/index.html#/?address=10.10.90.147&port=6288&protocol=http&objects=1244005232%2C-600117258%2C735464148%2C1267694822%2C-1330819611%2C-356815652%2C1979134216%2C59761832%2C665956269%2C-800897043%2C-1574492668%2C1757441197%2C-1085064717%2C1117981965%2C-1648462353%2C-1548780488%2C-1014868067%2C1046747083%2C-642678848%2C534205580%2C1564356121%2C-2111613189%2C-1137549524%2C-601918327%2C569980127&activesid=-1134906494&realtime=true&longterm=false&from=20251015134000&to=20251015135000&fromPast=true'
const paperDJUrl   = '/paper/index.html#/?address=10.10.47.147&port=6288&protocol=http&objects=1096671982%2C-1693901075%2C1142187535%2C2047176664%2C438846589%2C-405928917%2C-505835631%2C-2070659591%2C1535722779%2C1704479948%2C95774569%2C-128768193%2C779364899%2C-1216513127&activesid=767907323'
const paperICUrl   = '/paper/index.html#/?address=10.10.63.147&port=6288&protocol=http&objects=2110252259%2C549060667%2C2107845439%2C-1223716053%2C-422040562%2C1135274982%2C-588859854%2C-1364816572%2C751020064%2C1588054473%2C2002373097%2C373472969%2C-1484114961%2C2023181069%2C1183204058%2C648767871%2C-615700183%2C-296708013%2C-1228483658%2C-1206447877%2C1728735257%2C1494966734%2C959859307%2C-992998851&realtime=true&longterm=false&from=20251015140100&to=20251015141100&fromPast=true&activesid=1960629450'
const paperCMUrl   = '/paper/index.html#/?address=210.1.1.113&port=6110&protocol=http&objects=-1505446786%2C-1330107696%2C1043803443%2C-224580179%2C-128441164%2C888284202%2C1062069700%2C-1593990425%2C779265284%2C-489118310%2C-400708477%2C616016925&realtime=true&longterm=false&from=20251015134600&to=20251015135600&fromPast=true&activesid=810665691'
const paperIRISUrl = '/paper/index.html#/?address=10.92.28.7&port=6288&protocol=http&objects=-327129928%2C1379847976%2C1111955590%2C-532429307%2C-615626436%2C-138047396%2C1824506693%2C259494941%2C-1770076761%2C786245940%2C-1210812274%2C-172584193%2C500693787%2C-2065782111%2C-1204152775%2C557008771%2C-2102287767%2C330817049%2C-1967222877%2C2092433149%2C-440488121&activesid=502949976'
const paperDBOXUrl = '/paper/index.html#/?address=172.31.1.82&port=6288&protocol=http&objects=-502923898%2C1463269392%2C-765184385%2C1265461189%2C250889590%2C2064428092%2C-91317891%2C2009490015%2C-288410651%2C-1136955212&activesid=1747367386&realtime=true&longterm=false&from=20250519210700&to=20250519211700&fromPast=true'

// boot 페이지로 감싸기(저장소 초기화 후 대상 URL로 replace)
const boot = (target) => `/paper-boot.html?target=${encodeURIComponent(target)}`
const paperPHBootUrl   = computed(() => boot(paperPHUrl))
const paperDJBootUrl   = computed(() => boot(paperDJUrl))
const paperICBootUrl   = computed(() => boot(paperICUrl))
const paperCMBootUrl   = computed(() => boot(paperCMUrl))
const paperIRISBootUrl = computed(() => boot(paperIRISUrl))
const paperDBOXBootUrl = computed(() => boot(paperDBOXUrl))
</script>

<style scoped>
.dashboard-tab-wrapper { padding: 0 1rem; }
iframe { border-radius: 8px; background: #fff; }
</style>
