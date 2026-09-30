import test from 'node:test'
import assert from 'node:assert/strict'
import { canUseConsole, menusFor } from '../src/permissions.js'
test('students and anonymous users have no console access or menus',()=>{for(const user of [null,{}, {role:'STUDENT'}]){assert.equal(canUseConsole(user),false);assert.deepEqual(menusFor(user),[])}})
test('teacher only has activities menu',()=>{assert.deepEqual(menusFor({role:'TEACHER'}).map(m=>m.key),['activities'])})
test('administrator has analytics, settings, and feedback management menus',()=>{assert.deepEqual(menusFor({role:'ADMIN'}).map(m=>m.key),['activities','roles','accounts','analytics','settings','feedback'])})
