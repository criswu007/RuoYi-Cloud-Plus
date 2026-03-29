<template>
  <el-card class="merge-card">
    <div class="page-head">
      <div>
        <h3>标准地址合并</h3>
        <p>适用于同层级重复地址归并，目标地址层级必须高于待合并地址。</p>
      </div>
    </div>
    <el-form label-width="120px">
      <el-form-item label="源标准地址ID集合">
        <el-input
          v-model="sourceStandardAddressIds"
          placeholder="逗号分隔，例如 1,2,3"
        />
      </el-form-item>
      <el-form-item label="目标标准地址ID">
        <el-input v-model="targetStandardAddressId" placeholder="例如 10" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submit">合并</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script>
import { mergeStandardAddresses } from '../api/address';

export default {
  data() {
    return {
      sourceStandardAddressIds: '',
      targetStandardAddressId: ''
    };
  },
  methods: {
    async submit() {
      const sourceStandardAddressIds = this.sourceStandardAddressIds
        .split(',')
        .map(v => v.trim())
        .filter(Boolean)
        .map(v => Number(v));
      const targetStandardAddressId = Number(this.targetStandardAddressId);
      if (!sourceStandardAddressIds.length || !targetStandardAddressId) {
        this.$message.warning('请填写完整参数');
        return;
      }
      await mergeStandardAddresses(sourceStandardAddressIds, targetStandardAddressId);
      this.$message.success('合并完成');
    }
  }
};
</script>

<style scoped>
.merge-card {
  border-radius: 18px;
}
.page-head h3 {
  margin: 0 0 6px;
}
.page-head p {
  margin: 0 0 18px;
  color: #5f6f7f;
}
</style>
