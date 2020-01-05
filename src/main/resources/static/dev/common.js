function sleep(time) {
    return new Promise((resolve) => setTimeout(resolve, time));
}

function message(title, content, success) {
    let html = '<div class="ui {} message transition hidden"><i class="close icon"></i><div class="header">{}</div><p>{}</p></div>';
    html = format(html, [success ? 'positive' : 'negative', title, content]);
    let p = $('#messages');
    p.append(html);
    p = p.children('div:last');
    p.children('.close').bind('click', function () {
        p.transition({animation: 'fade down', duration: 300})
    });
    p.transition('fade left');
    sleep(5000).then(() => {
        if (p.hasClass('visible')) p.transition({animation: 'fade down', duration: 600})
    })
}

function msgBox(title, content, approve) {
    $('.modal .header').html(title);
    $('.modal .description').html(content);
    let modal = $('.modal');
    modal.modal({onApprove: approve});
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

function composeModify() {
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
    return data
}

function composeData() {
    let data = composeModify();
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
        else {
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

function selectionChanged() {
    let self = $(this);
    const names = {'flightId': 'type', 'type': 'fee', 'deal': 'fee'};
    let name = self.attr('name');
    let sel = self.val();
    let next = null;
    let val = null;
    if (names.hasOwnProperty(name)) {
        next = $(format('input[name="{}"]', names[name]));
        val = next.val()
    }
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
        val = '';
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

    if (next != null) {
        if (val !== next.val()) selectionChanged(next)
    }
}

function getRow(data) {
    let r = [data.name, data.pdbCptOn + data.pdbCptOff, data.pdbCptOn, data.pdbCpmOn + data.pdbCpmOff, data.pdbCpmOn,
        data.pdOn + data.pdOff, data.pdOn, data.bottomOn + data.bottomOff, data.bottomOn, 0,
        data.cpcOn + data.cpcOff, data.cpcOn, data.cpmOn + data.cpmOff, data.cpmOn];
    r[9] = r[1] + r[3] + r[5] + r[7];
    r.push(r[10] + r[12]);
    return r;
}

function initEvents() {
    $.fn.api.settings.api = {
        'create': '/j/create',
        'modify': '/j/modify',
        'query': '/j/flight/{query}',
        'table': '/j/table'
    };
    $('.ui.selection.dropdown').dropdown();
    $('.ui.dropdown.button').dropdown();
    for (let t of ['flightId', 'type', 'deal', 'fee'])
        $(format('input[name="{}"]', t)).bind('change', selectionChanged);
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
            $('#create').addClass('loading');
            return settings;
        },
        onResponse: function (response) {
            $('#create').removeClass('loading');
            if (response) {
                if (response.success) message('创建成功', '广告单创建成功！', true);
                else message('创建失败', response.message);
            } else message('创建失败', '无响应！');
            return response;
        },
        onError: function (errorMessage) {
            message('创建失败', errorMessage);
        }
    });

}

function initData() {
    let date = new Date();
    $('input[name="begin"]').val(parseDate(date, '-'));
    $('input[name="end"]').val('2021-12-31');
}

function bindTds(idx) {
    const deal1 = ['PDB-CPT', 'PDB-CPM', 'PD', '抄底'];
    const deal2 = ['CPC', 'CPM'];
    let tds = idx ? $('tbody').children('tr').eq(idx).nextAll().children('td') : $('td');
    tds.bind('click', function () {
        let self = $(this);
        let col = self.index();
        if (contains([0, 9, 14], col)) return;
        let val = parseInt(self.html());
        if (isNaN(val)) return;
        let flightName = self.prevAll().eq(0).html();
        let type = col < 9 ? 1 : 2;
        let deal = col > 4 ? (col > 6 ? 3 : 2) : 1;
        let fee = contains([1, 2, 7, 8, 10, 11], col) ? 1 : 2;
        let remove = col % 2 === (col < 9 ? 1 : 0);
        let title = (col < 9 ? '合约 ' + deal1[Math.floor((col - 1) / 2)] :
            '竞价 ' + deal2[Math.floor((col - 10) / 2)]) + (remove ? ' 广告总数' : ' 开启数量');
        msgBox(title, '<div class="ui fluid input"><input id="number" type="number" value="' + val + '"></div>',
            function () {
                let amount = parseInt($('#number').val());
                if (isNaN(amount) || amount < 0) {
                    message('操作失败', '请输入合法的数量');
                    return;
                }
                $(document).api({
                    on: 'now',
                    action: 'modify',
                    method: 'post',
                    beforeSend: function (settings) {
                        settings.data = {flightName, type, deal, fee, remove, amount};
                        self.html(amount);
                        return settings;
                    },
                    onResponse: function (response) {
                        if (response) {
                            if (response.success) {
                                message('操作完成', title + '修改成功', true);
                                initTable(type);
                                return
                            } else message('操作失败', response.message);
                        } else message('操作失败', '无响应');
                        self.html(val);
                        return response;
                    },
                    onError: function (errorMessage) {
                        message('操作失败', errorMessage);
                        self.html(val);
                    }
                })
            })
    })
}

function initTable(flag) {
    $(document).api({
        on: 'now',
        action: 'table',
        method: 'get',
        beforeSend: function (settings) {
            settings.data = {flag};
            return settings
        },
        onResponse: function (response) {
            if (response) {
                if (response.success) {
                    let total = [];
                    for (let i = 0; i < 15; i++) {
                        total.push(0);
                    }
                    let rows = {};
                    let tb = $('tbody');
                    let idx = 0;
                    tb.children('tr').each(function () {
                        let name = $(this).children('td:first').html();
                        rows[name] = $(this);
                        idx += 1;
                        if (!response.results.hasOwnProperty(name)) {
                            $(this).children('td').each(function (i) {
                                total[i] += i ? parseInt($(this).html()) : 1;
                            })
                        }
                    });
                    let flag = false;
                    for (let data of response.results) {
                        let r = getRow(data);
                        total[0] += 1;
                        for (let i = 1; i < 15; i++) {
                            total[i] += r[i];
                        }
                        if (rows.hasOwnProperty(data.name)) {
                            rows[data.name].children('td').each(function (i) {
                                $(this).html(r[i])
                            });
                        } else {
                            flag = true;
                            let html = '<tr><td class="left aligned">' + r[0] + '</td>';
                            for (let i = 1; i < 15; i++) {
                                html += '<td>' + r[i] + '</td>'
                            }
                            tb.append(html + '</tr>')
                        }
                    }
                    $('tfoot tr').children('th').each(function (i) {
                        $(this).html(total[i] + (i ? '' : ' 项'))
                    });
                    if (flag) bindTds(idx)
                } else message('更新失败', response.message);
            } else message('更新失败', '无响应');
            return response
        },
        onError: function (errorMessage) {
            message('更新失败', errorMessage);
        }
    })
}

$(document).ready(() => {
    initEvents();
    initData();
    //initTable(3)
});
