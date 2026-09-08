import test from 'node:test'
import assert from 'node:assert/strict'
import { canUseConsole, menusFor } from '../src/permissions.js'
test('students and anonymous users have no console access or menus',()=>{for(const user of [null,{}, {role:'STUDENT'}]){assert.equal(canUseConsole(user),false);assert.deepEqual(menusFor(user),[])}})
test('teacher only has activities and own accounts menu',()=>{assert.deepEqual(menusFor({role:'TEACHER'}).map(m=>m.key),['activities','accounts'])})
test('administrator has website settings in addition to management menus',()=>{assert.deepEqual(menusFor({role:'ADMIN'}).map(m=>m.key),['activities','roles','accounts','settings'])})
