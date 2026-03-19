import { ref, type Ref } from 'vue'
import type { FormInst } from 'naive-ui'

export function useForm<T extends Record<string, any>>(defaultValue: T) {
  const formValue = ref({ ...defaultValue }) as Ref<T>
  const formRef = ref<FormInst | null>(null)

  function resetForm() {
    formValue.value = { ...defaultValue }
    formRef.value?.restoreValidation()
  }

  async function validate(): Promise<boolean> {
    try {
      await formRef.value?.validate()
      return true
    } catch {
      return false
    }
  }

  return { formValue, formRef, resetForm, validate }
}
