/**
 * 密码加密本地调试脚本（仅调试用，业务代码不引入本文件）
 *
 * 目的：验证前端生成的 BCrypt 密文与后端 BCryptUtil（at.favre.lib:bcrypt, 10 rounds）
 *       完全互通，便于对照 init-data.sql 中的预置账号密码。
 *
 * 运行方式（任选其一）：
 *   1) 在 frontend 目录执行： npx tsx src/test/encryptTest.ts
 *   2) Node 22+/24 原生 TS 支持： node src/test/encryptTest.ts
 *
 * 注意：
 *   - BCrypt 每次加盐不同，同一明文多次加密结果不同，属正常现象；
 *   - 本脚本只在控制台打印，不写库、不发请求。
 */
import bcrypt from 'bcryptjs'

/** 与后端一致的加密强度（at.favre.lib BCrypt strength = 10） */
const SALT_ROUNDS = 10

/**
 * 加密函数：明文 → BCrypt 密文
 * @param plain 明文密码
 * @param rounds 加密轮数，默认 10（与后端一致）
 * @returns 形如 $2a$10$... 的 60 位密文
 */
export function encryptPassword(plain: string, rounds: number = SALT_ROUNDS): string {
  if (!plain) {
    throw new Error('明文密码不能为空')
  }
  return bcrypt.hashSync(plain, rounds)
}

/**
 * 校验函数：用密文比对明文（模拟后端 BCryptUtil.matches）
 * @param plain 明文密码
 * @param hashed 数据库中的 BCrypt 密文
 * @returns 是否匹配
 */
export function verifyPassword(plain: string, hashed: string): boolean {
  return bcrypt.compareSync(plain, hashed)
}

/* ============================== 测试示例 ============================== */

/** 测试用明文（与 init-data.sql 预置账号对应：alice/bob=123456，admin=admin123） */
const SAMPLES: string[] = ['123456', 'admin123']

console.log('==================== BCrypt 加密调试 ====================')
console.log(`加密轮数：${SALT_ROUNDS}（与后端 BCryptUtil 保持一致）`)
console.log('')

SAMPLES.forEach((plain, index) => {
  const cipher = encryptPassword(plain)

  console.log(`【示例 ${index + 1}】`)
  console.log(`明文：${plain}`)
  console.log(`密文：${cipher}`)
  console.log(`密文长度：${cipher.length}`)
  console.log(`回验结果：${verifyPassword(plain, cipher) ? '匹配 ✓' : '不匹配 ✗'}`)
  // 错误密码校验，预期 false
  console.log(`错误密码校验：${verifyPassword(plain + '_wrong', cipher) ? '匹配 ✗' : '不匹配 ✓'}`)
  console.log('')
})

// 演示「同一明文两次加密结果不同」（BCrypt 随机盐）
const first = encryptPassword('123456')
const second = encryptPassword('123456')
console.log('【随机盐演示】')
console.log(`第一次密文：${first}`)
console.log(`第二次密文：${second}`)
console.log(`两次密文是否相同：${first === second ? '相同' : '不同（正常，盐随机）'}`)
console.log(`两次密文均可通过校验：${verifyPassword('123456', first) && verifyPassword('123456', second)}`)
console.log('=========================================================')
