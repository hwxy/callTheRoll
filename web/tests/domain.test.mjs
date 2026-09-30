import test from 'node:test'
import assert from 'node:assert/strict'
import {levelFor,scaleFor,destinationFor,pets,requestKey} from '../src/lib/domain.mjs'
test('growth threshold is exactly ten points',()=>{assert.equal(levelFor(0),1);assert.equal(levelFor(9),1);assert.equal(levelFor(10),2);assert.equal(levelFor(99),10);assert.equal(levelFor(100),11)})
test('size grows to ten then caps, while levels keep growing',()=>{assert.ok(scaleFor(2)>scaleFor(1));assert.equal(scaleFor(10),scaleFor(100));assert.equal(scaleFor(0),scaleFor(1))})
test('front-end login currently routes teachers only, based on the server role',()=>{assert.equal(destinationFor('STUDENT'),null);assert.ok(destinationFor('TEACHER'));assert.equal(destinationFor('ADMIN'),null);assert.equal(destinationFor('OTHER'),null)})
test('three distinct pet choices and request ids',()=>{assert.equal(new Set(pets.map(p=>p.key)).size,3);assert.match(requestKey(),/^[A-Za-z0-9_-]{8,80}$/);assert.equal(new Set(Array.from({length:100},requestKey)).size,100)})
