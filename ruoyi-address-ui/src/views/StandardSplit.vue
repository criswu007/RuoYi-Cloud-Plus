<template>
  <el-card class="split-card">
    <div class="page-head">
      <div>
        <h3>标准地址拆分</h3>
        <p>将一个标准地址拆成多个同层级子项，适合道路分段、栋单元细化等场景。</p>
      </div>
    </div>
    <el-form label-width="120px">
      <el-form-item label="源标准地址ID">
        <el-input v-model="sourceStandardAddressId" placeholder="例如 1001" />
      </el-form-item>
      <el-form-item label="新地址列表">
        <div class="split-rows">
          <div v-for="(row, idx) in newAddresses" :key="idx" class="split-row">
            <el-input v-model="row.name" placeholder="新地址名称" />
            <el-button type="text" @click="removeRow(idx)">删除</el-button>
          </div>
        </div>
        <el-button type="primary" plain size="mini" @click="addRow">新增一行</el-button>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submit">拆分</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script>
import { splitStandardAddress } from '../api/address';

export default {
  data() {
    return {
      sourceStandardAddressId: '',
      newAddresses: [{ name: '' }]
    };
  },
  methods: {
    addRow() {
      this.newAddresses.push({ name: '' });
    },
    removeRow(idx) {
      this.newAddresses.splice(idx, 1);
      if (!this.newAddresses.length) {
        this.newAddresses.push({ name: '' });
      }
    },
    async submit() {
      const sourceStandardAddressId = Number(this.sourceStandardAddressId);
      const newAddresses = this.newAddresses
        .map(item => ({ name: item.name.trim() }))
        .filter(item => item.name);
      if (!sourceStandardAddressId || !newAddresses.length) {
        this.$message.warning('请填写完整参数');
        return;
      }
      await splitStandardAddress(sourceStandardAddressId, newAddresses);
      this.$message.success('拆分完成');
      this.sourceStandardAddressId = '';
      this.newAddresses = [{ name: '' }];
    }
  }
};
</script>

<style scoped>
.split-rows {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.split-row {
  display: flex;
  gap: 8px;
  align-items: center;
}
.split-card {
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
