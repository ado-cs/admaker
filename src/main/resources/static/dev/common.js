function sleep(time) {
    return new Promise((resolve) => setTimeout(resolve, time));
}

function message(content, success) {
    let html = '<div class="ui {} message transition hidden"><p>{}</p></div>';
    html = format(html, [success ? 'positive' : 'negative', content]);
    let p = $('#messages');
    p.append(html);
    p = p.children('div:last');
    p.transition('fade left');
    sleep(8000).then(() => {
        if (p.hasClass('visible')) p.transition({animation: 'fade down', duration: 600})
    })
}

function msgBox(title, content, approve) {
    let modal = $('.mini.modal');
    modal.children('.header').html(title);
    $('.mini.modal .description').html(content);
    modal.children('.actions').html(approve ?
        '<div class="ui negative deny button">取消</div><div class="ui positive right labeled icon approve button">确认<i class="checkmark icon"></i></div>' :
        '<div class="ui primary approve button">确认</div>');
    let props = {closable: false, onApprove: approve};
    if (approve && content.indexOf('<input') > 0) props.onVisible = function() {
        $('.mini.modal input:first').select()
    };
    modal.modal(props);
    modal.modal('show');
}

function parseDate(date, connector) {
    let s = connector ? connector : '';
    return date.getFullYear() + s + (date.getMonth() < 9 ? '0' : '') + (date.getMonth() + 1) + s + (date.getDate() < 10 ? '0' : '') + date.getDate()
}

function compareDate(date1, date2) {
    if (date1.constructor === String) date1 = new Date(date1);
    if (date2.constructor === String) date2 = new Date(date2);
    if (date1.getFullYear() !== date2.getFullYear()) return date1.getFullYear() > date2.getFullYear() ? 1 : -1;
    if (date1.getMonth() !== date2.getMonth()) return date1.getMonth() > date2.getMonth() ? 1 : -1;
    if (date1.getDate() !== date2.getDate()) return date1.getDate() > date2.getDate() ? 1 : -1;
    return 0
}

function divide(value, second) {
    if (!value) return '';
    let i = value.indexOf('_');
    if (i === -1) return '';
    return second ? value.substring(i + 1) : value.substring(0, i);
}

function format(str, val) {
    if (val.constructor === Array) {
        let i = 0;
        for (let v of val) {
            i = str.indexOf('{}', i);
            if (i === -1) break;
            str = str.substring(0, i) + v + str.substring(i + 2);
            i += v.length;
        }
        return str;
    }
    let i = str.indexOf('{}');
    return str.substring(0, i) + val + str.substring(i + 2);
}

function fillOptions(name, options) {
    let html = '';
    let idx = 1;
    for (let opt of options) {
        html += format('<option class="item" data-value="{}">{}</option>',
            opt.constructor === String ? [idx++, opt] : [opt.value, opt.name]);
    }
    let p = $(format('input[name="{}"]', name));
    p.nextAll('.menu').html(html);
    if (!options || options.length < 1) return;
    p.nextAll('.text').html(options[0].constructor === String ? options[0] : options[0].name);
    p.val(options[0].constructor === String ? '1' : options[0].value.toString())
}

function setDisabled(names, flag) {
    if (names.constructor === String) {
        names = [names];
    }
    for (let name of names) {
        let p = $(format('input[name="{}"]', name));
        if (!p) continue;
        p = p.parent();
        if (flag) {
            p.addClass('disabled');
            p.prev().attr('style', 'color: rgba(34, 36, 38, 0.15)')
        } else {
            p.removeClass('disabled');
            p.prev().removeAttr('style')
        }
    }
}

function contains(array, val) {
    for (let v of array) {
        if (v === val) return true;
    }
    return false;
}

function composeData() {
    let data = {};
    $('input').each(function () {
        let name = $(this).attr('name');
        if (name) {
            data[name] = $(this).val()
        }
    });
    if (!data.type) {
        msgBox('提示', '请选择广告类型');
        return false
    }
    data.flightName = $('input[name="flightId"]').nextAll('.text').html();
    if (!data.deal) data.deal = 1;
    if (!data.fee) data.fee = parseInt(data.type) === 1 && parseInt(data.deal) === 2 ? 2 : 1;
    data.flightType = divide(data.flightId, true);
    data.flightId = divide(data.flightId);
    if (!data.flightId) {
        msgBox('提示', '请选择广告位');
        return false
    }
    let amount = parseInt(data.amount);
    if (isNaN(amount) || amount < 1) {
        msgBox('提示', '请输入正确的广告数量');
        return false
    }
    if (!data.dspId) data.dspId = 10101;
    if (!data.flow) data.flow = 1;
    else data.flow = parseInt(data.flow);
    if (parseInt(data.fee) === 1 && amount > 1) {
        if (amount === 2 && contains([1, 4, 6, 9, 10], data.flow)) data.flow = 2;
        else if (amount === 3 && !contains([3, 5, 7], data.flow)) data.flow = 3;
        else if (amount === 4 && !contains([5, 7], data.flow)) data.flow = 5;
        else if (amount === 5 && !contains([7], data.flow)) data.flow = 7;
        else if (amount > 5) {
            msgBox('提示', '广告数量与CPT流量冲突');
            return false
        }
    }
    if (!data.showNumber || isNaN(parseInt(data.showNumber)) || parseInt(data.showNumber) < 10 || parseInt(data.showNumber) > 99999) data.showNumber = 10000;
    if (!data.showRadio || isNaN(parseInt(data.showRadio)) || parseInt(data.showRadio) <= 0 || parseInt(data.showRadio) > 100) data.showRadio = 0.4;
    else data.showRadio /= 100;
    let date = new Date();
    if (!data.begin) data.begin = parseDate(date, '-');
    if (!data.end) data.end = '2021-12-31';
    if (compareDate(data.begin, data.end) > 0) {
        msgBox('提示', '结束时间不能开始时间之前');
        return false
    }
    return data
}

function selectionChanged(self) {
    const names = {'flightId': 'type', 'type': 'fee', 'deal': 'fee'};
    let name = self.attr('name');
    let sel = self.val();
    let next = null;
    if (names.hasOwnProperty(name))
        next = $(format('input[name="{}"]', names[name]));

    if (name === 'flightId') {
        let v = divide(sel, true);
        let opt = [];
        if (v === '1' || v === '3') opt.push({name: '合约', value: 1});
        if (v === '2' || v === '3') opt.push({name: '竞价', value: 2});
        fillOptions('type', opt)
    } else if (name === 'type') {
        if (sel === '1') {
            setDisabled('deal', false);
            fillOptions('fee', ['CPT', 'CPM'])
        } else {
            setDisabled('deal', true);
            fillOptions('fee', ['CPC', 'CPM'])
        }
    } else if (name === 'deal') {
        if (sel === '1') {
            fillOptions('fee', ['CPT', 'CPM'])
        } else if (sel === '2') {
            fillOptions('fee', ['CPT'])
        } else {
            fillOptions('fee', [{name: 'CPM', value: 2}])
        }
    } else if (name === 'fee') {
        if ($('input[name="type"]').val() === '1') {
            setDisabled('flow', sel !== '1');
            setDisabled(['showNumber', 'showRadio'], false);
            let p = $('#show .ui.input input');
            if (sel === '1') {
                $('#show label').html('每日轮播比例');
                $('#show .ui.input .ui.label').html('%');
                p.val('');
                p.attr('name', 'showRadio');
                p.attr('placeholder', '轮播比例')
            } else {
                $('#show label').html('每日展示数量');
                $('#show .ui.input .ui.label').html('CPM');
                p.val('');
                p.attr('name', 'showNumber');
                p.attr('placeholder', '展示数量')
            }
        } else {
            setDisabled(['flow', 'showNumber', 'showRadio'], true);
        }
    }

    if (next != null) selectionChanged(next)
}

let refreshFlag = 0;

function initEvents() {
    $('.menu .item').tab();
    $.fn.api.settings.api = {
        'create': '/j/create',
        'modify': '/j/modify',
        'query': '/j/flight/{query}',
        'table': '/j/table'
    };
    $('.ui.selection.dropdown').dropdown();
    $('.ui.dropdown.button').dropdown();
    for (let t of ['flightId', 'type', 'deal', 'fee'])
        $(format('input[name="{}"]', t)).bind('change', function () {
            selectionChanged($(this))
        });
    $('#settings').bind('click', function () {
        let p = $('#more');
        if (p.hasClass('in')) return;
        let s = $(this).children('.icon');
        if (s.hasClass('up')) {
            s.removeClass('up');
            s.addClass('down')
        } else {
            s.removeClass('down');
            s.addClass('up')
        }
        p.transition('slide down')
    });
    $('.ui.search.dropdown').dropdown({
        minCharacters: 2,
        saveRemoteData: false,
        apiSettings: {
            action: 'query',
            beforeSend: function (settings) {
                if (settings.urlData.query.length < 2) return false;
                let n = 0;
                for (let i = 0; i < settings.urlData.query.length; i++) {
                    let j = settings.urlData.query.charCodeAt(i);
                    if (j < 128 && j !== 40 && j !== 41 && (j > 57 || j < 48)) n++;
                }
                return n > 1 ? false : settings
            },
            onResponse: function (response) {
                if (!response || !response.success || !response.results || response.results.length === 0)
                    fillOptions('flightId', []);
                return response;
            }
        }
    });
    $('#create').api({
        action: 'create',
        method: 'post',
        beforeSend: function (settings) {
            let data = composeData();
            if (!data) return false;
            settings.data = data;
            message('广告单创建请求已提交', true);
            return settings;
        },
        onResponse: function (response) {
            if (response) {
                if (response.success) {
                    refreshFlag = 3;
                    message('广告单创建成功！', true);
                }
                else message(response.message);
            } else message('无响应！');
            return response;
        },
        onError: function (errorMessage) {
            message(errorMessage);
        }
    });
    $('.menu .item[data-tab="second"]').bind('click', function () {
        initTable(refreshFlag);
        refreshFlag = 0;
    });
    let messages = $('#messages');
    $(window).scroll(function(){
        let t = $(document).scrollTop();
        if (parseInt(t) > 26)
            messages.css('top', t);
        else messages.css('top', '26px');
    })
}

function initData() {
    let date = new Date();
    $('input[name="begin"]').val(parseDate(date, '-'));
    $('input[name="end"]').val('2021-12-31');
}

let tableData = {};

function fillTable() {
    let total = [];
    for (let i = 0; i < 15; i++) total.push(0);
    let amount = [];
    for (let i = 0; i < 15; i++) amount.push(0);
    let idx = 0;
    let rows = {};
    let tbody = $('.ui.tab table tbody');
    tbody.children('tr').each(function () {
        let name = $(this).children('td:first').html();
        if (tableData.hasOwnProperty(name)) {
            rows[name] = $(this);
            idx += 1;
        }
        else $(this).remove()
    });
    let flag = false;
    for (let name in tableData) {
        let data = tableData[name];
        total[0] += 1;
        amount[0] = name;
        let sum1 = 0;
        let sum2 = 0;
        for (let i = 1; i < 15; i++) {
            let num = 0;
            if (data.hasOwnProperty(i)) {
                for (let id in data[i])
                    num += data[i][id]
            }
            if (i < 9 && i % 2 === 1) sum1 += num;
            else if (i > 9 && i % 2 === 0) sum2 += num;
            total[i] += num;
            amount[i] = num;
        }
        amount[9] = sum1;
        amount[14] = sum2;
        total[9] += sum1;
        total[14] += sum2;

        if (rows.hasOwnProperty(name)) {
            rows[name].children('td').each(function (i) {
                $(this).html(amount[i])
            });
        }
        else {
            flag = true;
            let html = '<tr><td class="left aligned">' + amount[0] + '</td>';
            for (let i = 1; i < 15; i++)
                html += '<td>' + amount[i] + '</td>';
            tbody.append(html + '</tr>')
        }
    }
    let html = '<tr><th class="left aligned">' + total[0] + ' 项</th>';
    for (let i = 1; i < 15; i++)
        html += '<th>' + total[i] + '</th>';
    $('.ui.tab table tfoot').html(html + '</tr>');
    bindThs();
    if (flag) bindTds(idx)
}

function showDsp(title, idx, data) {
    let table = '<table class="ui celled table"><thead><tr><th>DspId</th><th>';
    let tableEnd = '</tbody></table>';
    if (idx) {
        table += '数量</th></tr></thead><tbody>';
        let rows = tableData[data.name][idx];
        for (let id in rows) {
            table += '<tr><td>' + id +
                '</td><td><input type="text" style="border-width: 0" value="' + rows[id] + '"></td></tr>'
        }
        msgBox(title, table + tableEnd, function () {
            $('.modal table tbody tr').each(function () {
                let id = parseInt($(this).children('td:first').html());
                let amount = parseInt($(this).children('td:last').children('input').val());
                if (rows[id] !== amount) {
                    data.dspId = id;
                    data.amount = amount;
                    doModify(data)
                }
            })
        })
    }
    else {
        // 暂不支持
        // data = {name: title, type: 1};
        // let rows = tableData[data.name];
        // for (let i = 1; i < 9; i++) {
        //
        // }
    }
}

function doModify(data) {
    $(document).api({
        on: 'now',
        action: 'modify',
        method: 'post',
        beforeSend: function (settings) {
            settings.data = data;
            return settings;
        },
        onResponse: function (response) {
            if (response) {
                if (response.success) {
                    message('操作成功！', true);
                    initTable(data.type);
                }
                else message(response.message);
            } else message('无响应');
            return response;
        },
        onError: function (errorMessage) {
            message(errorMessage);
        }
    })
}

function quzModify(content, data) {
    msgBox('请确认', content, function () {
        doModify(data)
    })
}

function numModify(title, data) {
    msgBox(title, '<div class="ui fluid input"><input type="number" value="' + data.amount + '"></div>', function () {
        let val = parseInt($('.modal input').val());
        if (data.amount > val) {
            data.amount = val;
            doModify(data);
        }
    })
}

function preModify(self, name) {
    let idx = self.index();
    if (!idx) {
        showDsp(name);
        return;
    }
    const text1 = ['PDB-CPT', 'PDB-CPM', 'PD', '抄底', '合约'];
    const text2 = ['CPC', 'CPM', '竞价'];
    let amount = parseInt(self.html());
    let type = idx < 10 ? 1 : 2;
    let remove = idx % 2 === (idx < 10 ? 1 : 0);
    if (remove && amount <= 0) return;
    if (!remove) {
        let num = parseInt(self.prev().html());
        if (num === 0) return;
        else if (name == null) amount = amount ? 0 : num;
    }
    let data = {name, type, remove, amount};
    if (idx !== 9 && idx !== 14) {
        if (type === 1) data.deal = idx > 4 ? (idx > 6 ? 2 : 3) : 1;
        data.fee = contains([1, 2, 7, 8, 10, 11], idx) ? 1 : 2;
        if (name) {
            let text = (remove ? '删除' : '开启') + '"' + name + '"';
            if (idx < 9) showDsp(text + text1[Math.floor((idx - 1) / 2)], idx, data);
            else numModify(text + text2[Math.floor((idx - 10) / 2)], data);
            return;
        }
    }
    if (remove) data.amount = 0;
    quzModify('确认' + (remove ? '<b>删除</b>' : (amount ? '开启' : '关闭')) +
        '所有' + (idx < 10 ? text1[Math.floor((idx - 1) / 2)] + "排期和" :
            text2[Math.floor((idx - 10) / 2)]) + "广告？", data)
}

function bindThs() {
    let th = $('.ui.tab table tfoot tr th');
    th.bind('click', function () {
        preModify($(this))
    })
}

function bindTds(idx) {
    let tds = idx ? $('.ui.tab table tbody').children('tr').eq(idx - 1).nextAll().children('td') : $('td');
    tds.each(function () {
        let self = $(this);
        let idx = self.index();
        let name = idx ? self.prevAll('td:last').html() : self.html();
        self.bind('click', function () {
            preModify(self, name)
        })
    });
}

function initTable(flag) {
    $(document).api({
        on: 'now',
        action: 'table',
        method: 'get',
        beforeSend: function (settings) {
            settings.data = {flag: flag ? flag : 0};
            return settings
        },
        onResponse: function (response) {
            if (response) {
                if (response.success) {
                    tableData = response.results;
                    fillTable()
                } else message(response.message);
            } else message('无响应');
            return response
        },
        onError: function (errorMessage) {
            message(errorMessage);
        }
    })
}

$(document).ready(() => {
    initEvents();
    initData();
    initTable(0);
});
