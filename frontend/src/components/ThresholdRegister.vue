<template>
  <div>
    <h2>임계치 등록</h2>
    <form @submit.prevent="registerThreshold">
      <div>
        <label for="dbType">DB 타입:</label>
        <input id="dbType" v-model="threshold.dbType" type="text" required />
      </div>
      <div>
        <label for="dbName">DB 이름:</label>
        <input id="dbName" v-model="threshold.dbName" type="text" required />
      </div>
      <div>
        <label for="tablespaceName">Tablespace 이름:</label>
        <input id="tablespaceName" v-model="threshold.tablespaceName" type="text" required />
      </div>
      <div>
        <label for="thresMb">임계치 (MB):</label>
        <input id="thresMb" v-model="threshold.thresMb" type="number" required />
      </div>
      <div style="margin-top: 10px;">
        <button type="submit" style="padding: 5px 10px; background-color: #FF5722; color: white; border: none; border-radius: 4px; cursor: pointer;">
          등록
        </button>
      </div>
    </form>
  </div>
</template>

<script>
import api from "@/api"; // 공통 axios 인스턴스 가져오기

export default {
  data() {
    return {
      threshold: {
        dbType: "",
        dbName: "",
        tablespaceName: "",
        thresMb: null,
      },
    };
  },
  methods: {
    // 임계치 등록
    registerThreshold() {
      api.post("/api/threshold", this.threshold)
        .then((response) => {
          if (response.status === 200) {
            alert("등록 완료!");
            this.$router.push({ name: "ThresholdList" }); // 등록 후 리스트로 이동
          }
        })
        .catch((error) => {
          console.error("등록 오류:", error);
          alert("등록 실패. 다시 시도해주세요.");
        });
    },
  },
};
</script>

<style scoped>
h2 {
  color: #FF5722;
}
form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
label {
  margin-bottom: 5px;
}
input {
  padding: 5px;
  width: 300px;
  border: 1px solid #ccc;
  border-radius: 4px;
}
</style>
